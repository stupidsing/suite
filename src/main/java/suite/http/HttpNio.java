package suite.http;

import static primal.statics.Fail.fail;
import static primal.statics.Rethrow.ex;

import java.io.IOException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import primal.MoreVerbs.Pull;
import primal.MoreVerbs.Read;
import primal.MoreVerbs.Split;
import primal.Nouns.Utf8;
import primal.NullableSyncQueue;
import primal.Verbs.Equals;
import primal.Verbs.Th;
import primal.adt.FixieArray;
import primal.adt.Opt;
import primal.fp.Funs.Sink;
import primal.fp.Funs.Source;
import primal.fp.Funs2.Fun2;
import primal.primitive.adt.Bytes;
import primal.puller.Puller;
import primal.statics.Rethrow.SinkIo;
import suite.http.Http.Handler;
import suite.http.Http.Header;
import suite.http.Http.Request;
import suite.http.Http.Response;
import suite.os.ListenNio;
import suite.os.ListenNio.IoAsync;

// mvn compile exec:java -Dexec.mainClass=suite.http.HttpNio
public class HttpNio {

	public static void main(String[] args) {
		new HttpNio().run(8051, request -> Response.of(Http.S200, "Contents"));
	}

	public void run(int port, Handler handleRequest) {
		Source<IoAsync> handleIo = () -> new IoAsync() {
			private Bytes bytes = Bytes.empty;
			private SinkIo<Puller<Bytes>> registerWrite;

			private Source<Boolean> eater = () -> parseLine(line -> handleRequest1stLine(line.trim(), o -> {
				try {
					registerWrite.f(o);
				} catch (IOException ex) {
					fail(ex);
				}
			}));

			public void read(Bytes in) {
				if (in != null) {
					bytes = bytes.append(in);
					while (eater.g())
						;
				}
			}

			public void registerWrite(SinkIo<Puller<Bytes>> sink) {
				registerWrite = sink;
			}

			private void handleRequest1stLine(String line, Sink<Puller<Bytes>> cb) {
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

			private void handleRequestBody(String line0, List<String> headerLines, Sink<Puller<Bytes>> cb) {
				eater = () -> FixieArray //
						.of(line0.split(" ")) //
						.map((method, url, proto) -> handleRequestBody(proto, method, url, headerLines, cb));
			}

			private boolean handleRequestBody( //
					String proto, //
					String method, //
					String url, //
					List<String> lines, //
					Sink<Puller<Bytes>> cb) {
				var headers = Read //
						.from(lines) //
						.fold(new Header(), (headers_, line_) -> Split //
								.strl(line_, ":") //
								.map((k, v) -> headers_.put(k, v)));

				Fun2<String, String, Request> requestFun = (host, pqs) -> Split.strl(pqs, "?").map((path0, query) -> {
					var path1 = path0.startsWith("/") ? path0 : "/" + path0;
					var path2 = ex(() -> URLDecoder.decode(path1, Utf8.charset));

					return Equals.string(proto, "HTTP/1.1") //
							? new Request(method, host, path2, query, headers, null) //
							: fail("only HTTP/1.1 is supported");
				});

				var pp = Split.string(url, "://");
				var request = pp != null ? Split.strl(pp.v, "/").map(requestFun) : requestFun.apply("", url);

				var cl = request.headers.getOpt("Content-Length").map(Long::parseLong);
				var te = Equals.ab(request.headers.getOpt("Transfer-Encoding"), Opt.of("chunked"));

				if (te)
					eater = handleChunkedRequestBody(chunks -> response(handleRequest.handle(request)));
				else if (cl.hasValue())
					eater = handleRequestBody(request, cl.g(), cb);
				else if (Set.of("DELETE", "GET", "HEAD").contains(request.method))
					eater = handleRequestBody(request, 0, cb);
				else
					eater = handleRequestBody(request, Long.MAX_VALUE, cb);

				return true;
			}

			private Source<Boolean> handleRequestBody(Request request, long contentLength, Sink<Puller<Bytes>> cb) {
				return new Source<>() {
					private int n;

					public Boolean g() {
						if (bytes != null)
							n += bytes.size();
						var isCompleteRequest = bytes == null || contentLength <= n;
						if (isCompleteRequest)
							cb.f(response(handleRequest.handle(request)));
						return !isCompleteRequest;
					}
				};
			}

			private Source<Boolean> handleChunkedRequestBody(Sink<List<Bytes>> cb) {
				var chunks = new ArrayList<Bytes>();

				return () -> {
					for (var i0 = 0; i0 < bytes.size(); i0++)
						if (bytes.get(i0) == 10) {
							var line = new String(bytes.range(0, i0).toArray(), Utf8.charset);
							var size = Integer.parseInt(line.trim(), 16);

							for (var i1 = i0 + 1 + size; i1 < bytes.size(); i1++)
								if (bytes.get(i1) == 10) {
									chunks.add(bytes.range(i0 + 1, i1));
									bytes = bytes.range(i1);
									return true;
								}

							if (size == 0)
								cb.f(chunks);
						}

					return false;
				};
			}

			private boolean parseLine(Sink<String> handleLine) {
				for (var i = 0; i < bytes.size(); i++)
					if (bytes.get(i) == 10) {
						var line = new String(bytes.range(0, i).toArray(), Utf8.charset);
						bytes = bytes.range(i + 1);
						handleLine.f(line);
						return true;
					}

				return false;
			}

			private Puller<Bytes> response(Response response) {
				Puller<Bytes> body;

				if (response.body != null)
					body = response.body;
				else {
					var queue = new NullableSyncQueue<Bytes>();
					new Th(() -> response.write.f(queue::offerQuietly)).start();
					body = Puller.of(queue::takeQuietly);
				}

				return Puller.concat( //
						Pull.from("HTTP/1.1 " + response.status + "\r\n"), //
						Pull.from(response.headers //
								.streamlet() //
								.map((k, v) -> k + ": " + v + "\r\n") //
								.toJoinedString()), //
						Pull.from("\r\n"), //
						body);
			}
		};

		new ListenNio(handleIo).run(port);
	}

}
