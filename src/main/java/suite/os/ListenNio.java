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

public class ListenNio {

	public static void main(String[] args) {
		var listen = new ListenNio();

		listen.handle = () -> new IoAsync() {
			private Bytes bytes = Bytes.empty;
			private String[] methodUrlProtocol;
			private List<String> lines;
			private Sink<Bytes> handleRequestBody;
			private Puller<Bytes> out;

			public Puller<Bytes> read(Bytes in) {
				if (in == null)
					handleRequestBody.f(in);
				else {
					bytes = bytes.append(in);

					re: while (handleRequestBody == null) {
						var i = 0;

						while (i < bytes.size())
							if (bytes.get(i) != 10)
								i++;
							else {
								var line = new String(bytes.range(0, i).toArray(), Utf8.charset);
								bytes = bytes.range(i + 1);
								handleRequestBody = handleRequestLine(line.trim());
								continue re;
							}

						return out;
					}

					handleRequestBody.f(bytes);
				}

				return out;
			}

			private Sink<Bytes> handleRequestLine(String line) {
				if (line.isEmpty()) {
					var headers = Read //
							.from(lines) //
							.fold(new Header(), (headers_, line_) -> Split //
									.strl(line_, ":") //
									.map((k, v) -> headers_.put(k, v)));

					var request = FixieArray.of(methodUrlProtocol).map((method, url, protocol) -> {
						Fun2<String, String, Request> requestFun = (host, pqs) -> Split.strl(pqs, "?")
								.map((path0, query) -> {
									var path1 = path0.startsWith("/") ? path0 : "/" + path0;
									var path2 = ex(() -> URLDecoder.decode(path1, Utf8.charset));

									return Equals.string(protocol, "HTTP/1.1") //
											? new Request(method, host, path2, query, headers, null) //
											: fail("only HTTP/1.1 is supported");
								});

						var pp = Split.string(url, "://");
						return pp != null ? Split.strl(pp.v, "/").map(requestFun) : requestFun.apply("", url);
					});

					return handleRequestBody(request);
				} else if (methodUrlProtocol != null)
					lines.add(line);
				else {
					methodUrlProtocol = line.split(" ");
					lines = new ArrayList<>();
				}

				return null;
			}

			private Sink<Bytes> handleRequestBody(Request request) {
				var cl = request.headers.getOpt("Content-Length").map(Long::parseLong);
				var te = Equals.ab(request.headers.getOpt("Transfer-Encoding"), Opt.of("chunked"));
				long requestBodyLength;

				if (te)
					return handleChunkedRequestBody();
				else if (cl.hasValue())
					requestBodyLength = cl.g();
				else if (Set.of("DELETE", "GET", "HEAD").contains(request.method))
					requestBodyLength = 0;
				else
					requestBodyLength = Long.MAX_VALUE;

				return handleRequestBody(requestBodyLength);
			}

			private Sink<Bytes> handleChunkedRequestBody() {
				return new Sink<>() {
					private Bytes bytes = Bytes.empty;
					private List<Bytes> chunks = new ArrayList<>();

					public void f(Bytes in) {
						bytes = bytes.append(in);

						re: while (true) {
							for (var i0 = 0; i0 < bytes.size(); i0++)
								if (bytes.get(i0) == 10) {
									var line = new String(bytes.range(0, i0).toArray(), Utf8.charset).trim();
									var size = Integer.parseInt(line, 16);
									var i1 = i0 + 1 + size;

									for (; i1 < bytes.size(); i1++)
										if (bytes.get(i1) == 10) {
											chunks.add(bytes.range(i0 + 1, i1));
											bytes = bytes.range(i1);
											continue re;
										}
								}

							break;
						}
					}
				};
			}

			private Sink<Bytes> handleRequestBody(long requestBodyLength) {
				var status = Http.S200;
				var body = "Contents";
				var response = Response.of(status, body);

				return new Sink<>() {
					private int n;

					public void f(Bytes bytes) {
						if (bytes != null)
							n += bytes.size();

						if (bytes == null || requestBodyLength <= n)
							out = response(response);
					}
				};
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
