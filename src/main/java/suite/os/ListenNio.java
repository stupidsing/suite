package suite.os;

import static primal.statics.Fail.fail;
import static primal.statics.Rethrow.ex;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import primal.MoreVerbs.Pull;
import primal.MoreVerbs.Read;
import primal.MoreVerbs.Split;
import primal.Nouns.Utf8;
import primal.Verbs.Equals;
import primal.adt.FixieArray;
import primal.adt.Opt;
import primal.fp.Funs.Sink;
import primal.fp.Funs.Source;
import primal.fp.Funs2.Fun2;
import primal.primitive.adt.Bytes;
import primal.puller.Puller;
import suite.http.Http;
import suite.http.Http.Header;
import suite.http.Http.Request;
import suite.http.Http.Response;

// mvn compile exec:java -Dexec.mainClass=suite.os.ListenNio
public class ListenNio {

	public static void main(String[] args) {
		var listen = new ListenNio();

		listen.handle = () -> new IoAsync() {
			private Bytes bytes = Bytes.empty;
			private Source<Boolean> eater = () -> parseLine(line -> handleRequest1stLine(line.trim(), o -> out = o));
			private Puller<Bytes> out;

			public Puller<Bytes> read(Bytes in) {
				bytes = bytes.append(in);
				while (eater.g())
					;
				return out;
			}

			private void handleRequest1stLine(String line, Sink<Puller<Bytes>> callback) {
				var hrhl = handleRequestHeaderLine(lines -> handleRequestBody(line, lines, callback));
				eater = () -> parseLine(hrhl);
			}

			private Sink<String> handleRequestHeaderLine(Sink<List<String>> callback) {
				var lines = new ArrayList<String>();

				return line0 -> {
					var line1 = line0.trim();

					if (!line1.isEmpty())
						lines.add(line1);
					else
						callback.f(lines);
				};
			}

			private void handleRequestBody(String line0, List<String> headerLines, Sink<Puller<Bytes>> callback) {
				eater = () -> FixieArray //
						.of(line0.split(" ")) //
						.map((method, url, proto) -> handleRequestBody(proto, method, url, headerLines, callback));
			}

			private boolean handleRequestBody( //
					String proto, //
					String method, //
					String url, //
					List<String> lines, //
					Sink<Puller<Bytes>> callback) {
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
					eater = handleChunkedRequestBody(chunks -> response(getResponse(request)));
				else if (cl.hasValue())
					eater = handleRequestBody(request, cl.g(), callback);
				else if (Set.of("DELETE", "GET", "HEAD").contains(request.method))
					eater = handleRequestBody(request, 0, callback);
				else
					eater = handleRequestBody(request, Long.MAX_VALUE, callback);

				return true;
			}

			private Source<Boolean> handleRequestBody( //
					Request request, //
					long contentLength, //
					Sink<Puller<Bytes>> callback) {
				return new Source<>() {
					private int n;

					public Boolean g() {
						if (bytes != null)
							n += bytes.size();
						var isCompleteRequest = bytes == null || contentLength <= n;
						if (isCompleteRequest)
							callback.f(response(getResponse(request)));
						return !isCompleteRequest;
					}
				};
			}

			private Source<Boolean> handleChunkedRequestBody(Sink<List<Bytes>> callback) {
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
								callback.f(chunks);
						}

					return false;
				};
			}

			private boolean parseLine(Sink<String> handleLine) {
				var i = 0;

				while (i < bytes.size())
					if (bytes.get(i) != 10)
						i++;
					else {
						var line = new String(bytes.range(0, i).toArray(), Utf8.charset);
						bytes = bytes.range(i + 1);
						handleLine.f(line);
						return true;
					}

				return false;
			}

			private Puller<Bytes> response(Response response) {
				return Puller.concat( //
						Pull.from("HTTP/1.1 " + response.status + "\r\n"), //
						Pull.from(response.headers //
								.streamlet() //
								.map((k, v) -> k + ": " + v + "\r\n") //
								.toJoinedString()), //
						Pull.from("\r\n"), //
						response.out);
			}

			private Response getResponse(Request request) {
				return Response.of(Http.S200, "Contents");
			}
		};

		listen.run();
	}

	public interface IoAsync {
		public Puller<Bytes> read(Bytes in);
	}

	private Source<IoAsync> handle;
	private Selector selector;

	public void run() {
		try {
			selector = Selector.open();

			// we have to set connection host, port and non-blocking mode
			var ssc = ServerSocketChannel.open();
			ssc.configureBlocking(false);
			ssc.register(selector, ssc.validOps(), null);

			var ss = ssc.socket();
			ss.bind(new InetSocketAddress("localhost", 8051));

			while (true) {
				selector.select();
				var iter = selector.selectedKeys().iterator();

				while (iter.hasNext()) {
					var key = iter.next();
					iter.remove();

					if (key.isAcceptable())
						handleAccept(ssc, key);
					if (key.isConnectable())
						;
					if (key.isReadable())
						handleRead((SocketChannel) key.channel(), (IoAsync) key.attachment());
					if (key.isWritable())
						handleWrite((SocketChannel) key.channel(), (Puller<?>) key.attachment());
				}
			}
		} catch (IOException ex) {
			throw new RuntimeException(ex);
		}
	}

	private void handleAccept(ServerSocketChannel ssc, SelectionKey key) throws IOException {
		var sc = ssc.accept();
		sc.configureBlocking(false);

		sc.register(selector, SelectionKey.OP_READ, handle.g());
	}

	private void handleRead(SocketChannel sc, IoAsync io) throws IOException {
		var bs = new byte[1024];
		var n = sc.read(ByteBuffer.wrap(bs));
		var puller = io.read(0 < n ? Bytes.of(bs, 0, n) : null);

		if (puller != null)
			sc.register(selector, SelectionKey.OP_WRITE, puller);
	}

	private void handleWrite(SocketChannel sc, Puller<?> puller) throws IOException {
		var bs = (Bytes) puller.pull();

		if (bs != null) {
			sc.write(ByteBuffer.wrap(bs.bs, bs.start, bs.end));
			sc.register(selector, SelectionKey.OP_WRITE, puller);
		} else
			sc.close();
	}

}
