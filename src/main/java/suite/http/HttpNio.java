package suite.http;

import static primal.statics.Fail.fail;
import static primal.statics.Rethrow.ex;

import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;

import primal.MoreVerbs.Pull;
import primal.MoreVerbs.Read;
import primal.MoreVerbs.Split;
import primal.Nouns.Buffer;
import primal.Nouns.Utf8;
import primal.Verbs.Equals;
import primal.adt.FixieArray;
import primal.adt.Opt;
import primal.fp.Funs.Sink;
import primal.fp.Funs.Source;
import primal.fp.Funs2.Fun2;
import primal.os.Log_;
import primal.primitive.adt.Bytes;
import primal.puller.Puller;
import suite.http.Http.Handler;
import suite.http.Http.Header;
import suite.http.Http.Request;
import suite.http.Http.Response;
import suite.os.ListenNio;
import suite.os.ListenNio.Reg;

// mvn compile exec:java -Dexec.mainClass=suite.http.HttpNio
public class HttpNio {

	public final ListenNio listen;
	private Handler handler;

	public HttpNio(Handler handler) {
		listen = new ListenNio(this::listen);
		this.handler = handler;
	}

	public void run(int port) {
		listen.run(port);
	}

	private void listen(Reg reg) {
		var rw = new Object() {
			private Bytes br = Bytes.empty;
			private Bytes bw = Bytes.empty;

			private Source<Boolean> eater = () -> parseLine(
					line -> handleRequest1stLine(line.trim(), o -> write = response(o)));

			private Puller<Bytes> write;

			private void read(Bytes in) {
				if (in != null) {
					br = br.append(in);
					while (eater.g())
						;
				} else
					write = Puller.empty(); // closes connection
			}

			private Bytes write() {
				if (bw != null && bw.isEmpty())
					bw = write.pull();
				return bw;
			}

			private void written(int n) {
				bw = bw.range(n);
			}

			private void handleRequest1stLine(String line, Sink<Response> cb) {
				var hrhl = handleRequestHeaderLine(lines -> handleRequestBody(line, lines, cb));
				eater = () -> parseLine(hrhl);
			}

			private Sink<String> handleRequestHeaderLine(Sink<List<String>> cb) {
				var lines = new ArrayList<String>();

				return line0 -> {
					var line1 = line0.trim();

					if (!line1.isEmpty())
						lines.add(line1);
					else
						cb.f(lines);
				};
			}

			private void handleRequestBody(String line0, List<String> headerLines, Sink<Response> cb) {
				eater = () -> FixieArray //
						.of(line0.split(" ")) //
						.map((method, url, proto) -> handleRequestBody(proto, method, url, headerLines, cb));
			}

			private boolean handleRequestBody( //
					String proto, //
					String method, //
					String url, //
					List<String> lines, //
					Sink<Response> cb) {
				var headers = Read //
						.from(lines) //
						.fold(new Header(), (headers_, line_) -> Split //
								.strl(line_, ":") //
								.map((k, v) -> headers_.put(k, v)));

				var queue = new ArrayBlockingQueue<Bytes>(Buffer.size);
				Sink<Bytes> offer = queue::add;
				Source<Bytes> take = queue::poll;

				Fun2<String, String, Request> requestFun = (host, pqs) -> Split.strl(pqs, "?").map((path0, query) -> {
					var path1 = path0.startsWith("/") ? path0 : "/" + path0;
					var path2 = ex(() -> URLDecoder.decode(path1, Utf8.charset));

					return Equals.string(proto, "HTTP/1.1") //
							? new Request(method, host, path2, query, headers, Puller.of(take)) //
							: fail("only HTTP/1.1 is supported");
				});

				var pp = Split.string(url, "://");
				var request = pp != null ? Split.strl(pp.v, "/").map(requestFun) : requestFun.apply("", url);

				var cl = request.headers.getOpt("Content-Length").map(Long::parseLong);
				var te = Equals.ab(request.headers.getOpt("Transfer-Encoding"), Opt.of("chunked"));
				Log_.info(request.getLogString());

				if (te)
					eater = handleChunkedRequestBody(request, offer, cb);
				else if (cl.hasValue())
					eater = handleRequestBody(request, offer, cl.g(), cb);
				else
					eater = handleRequestBody(request, offer, 0, cb);

				return true;
			}

			private Source<Boolean> handleRequestBody( //
					Request request, //
					Sink<Bytes> body, //
					long contentLength, //
					Sink<Response> cb) {
				return new Source<>() {
					private int n;

					public Boolean g() {
						body.f(br);
						var isOpen = br != null;
						if (isOpen) {
							n += br.size();
							br = Bytes.empty;
						}
						if (!isOpen || contentLength <= n)
							cb.f(handler.handle(request));
						return false;
					}
				};
			}

			private Source<Boolean> handleChunkedRequestBody(Request request, Sink<Bytes> body, Sink<Response> cb) {
				return () -> {
					for (var i0 = 0; i0 < br.size(); i0++)
						if (br.get(i0) == 10) {
							var line = new String(br.range(0, i0).toArray(), Utf8.charset);
							var size = Integer.parseInt(line.trim(), 16);

							for (var i1 = i0 + 1 + size; i1 < br.size(); i1++)
								if (br.get(i1) == 10) {
									var chunk = br.range(i0 + 1, i1);
									br = br.range(i1);
									body.f(chunk);
									return true;
								}

							if (size == 0)
								cb.f(handler.handle(request));
						}

					return false;
				};
			}

			private boolean parseLine(Sink<String> handleLine) {
				for (var i = 0; i < br.size(); i++)
					if (br.get(i) == 10) {
						var line = new String(br.range(0, i).toArray(), Utf8.charset);
						br = br.range(i + 1);
						handleLine.f(line);
						return true;
					}

				return false;
			}
		};

		new Object() {
			private void listen() {
				if (rw.write == null)
					reg.listenRead(in -> {
						rw.read(in);
						listen();
					});
				else
					reg.listenWrite(rw::write, n -> {
						rw.written(n);
						listen();
					});
			}
		}.listen();
	}

	private Puller<Bytes> response(Response response) {
		Puller<Bytes> responseBody;

		if (response.body != null)
			responseBody = response.body;
		else
			responseBody = response.body;
			// response.write.f(offer);

		return Puller.concat( //
				Pull.from("HTTP/1.1 " + response.status + "\r\n" //
						+ response.headers //
								.streamlet() //
								.map((k, v) -> k + ": " + v + "\r\n") //
								.toJoinedString() //
						+ "\r\n"), //
				responseBody);
	}

}
