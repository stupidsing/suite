package suite.http;

import static suite.util.Friends.min;

import java.io.IOException;
import java.io.InputStream;
import java.net.URLDecoder;
import java.util.HashMap;

import suite.Constants;
import suite.os.LogUtil;
import suite.os.SocketUtil;
import suite.streamlet.Read;
import suite.util.Copy;
import suite.util.Fail;
import suite.util.FunUtil2.Fun2;
import suite.util.Rethrow;
import suite.util.String_;
import suite.util.To;
import suite.util.Util;

/**
 * A very crude HTTP server.
 *
 * Possible improvements:
 *
 * TODO persistent connection
 *
 * TODO direct output without buffering
 *
 * @author yw.sing
 */
public class HttpServe {

	private int port;

	public HttpServe() {
		this(8051);
	}

	public HttpServe(int port) {
		this.port = port;
	}

	public void serve(HttpHandler handler) {
		try {
			serve_(handler);
		} catch (IOException ex) {
			Fail.t(ex);
		}
	}

	private void serve_(HttpHandler handler) throws IOException {
		new SocketUtil().listenIo(port, (is, os) -> {
			var ls = Util.readLine(is).split(" ");
			var method = ls[0];
			var url = ls[1];
			var protocol = ls[2];

			Fun2<String, String, HttpRequest> requestFun = (host, pqs) -> String_.split2l(pqs, "?").map((path, query) -> {
				var path1 = path.startsWith("/") ? path : "/" + path;
				var path2 = Rethrow.ex(() -> URLDecoder.decode(path1, "UTF-8"));

				if (String_.equals(protocol, "HTTP/1.1")) {
					var requestHeaders = new HashMap<String, String>();
					String line;

					while (!(line = Util.readLine(is)).isEmpty())
						String_.split2l(line, ":").map(requestHeaders::put);

					var cls = requestHeaders.get("Content-Length");
					var contentLength = cls != null ? Integer.parseInt(cls) : 0;
					var cis = sizeLimitedInputStream(is, contentLength);

					return new HttpRequest(method, host, path2, query, requestHeaders, cis);
				} else
					return Fail.t("only HTTP/1.1 is supported");
			});

			var pp = String_.split2(url, "://");
			var request = pp != null ? String_.split2l(pp.t1, "/").map(requestFun) : requestFun.apply("", url);
			HttpResponse response = null;

			try {
				response = handler.handle(request);
			} catch (Exception ex) {
				LogUtil.error(ex);
				response = HttpResponse.of(HttpResponse.HTTP500);
			} finally {
				LogUtil.info(request.getLogString() + " " + response.getLogString());
			}

			var sb = new StringBuilder();

			sb.append("HTTP/1.1 " + response.status + "\r\n");
			Read.from2(response.headers).sink((k, v) -> sb.append(k + ": " + v + "\r\n"));
			sb.append("\r\n");

			os.write(sb.toString().getBytes(Constants.charset));
			Copy.stream(response.out.collect(To::inputStream), os);
		});
	}

	private InputStream sizeLimitedInputStream(InputStream is, int size) {
		return new BasicInputStream(is) {
			private int remaining = size;

			public int read() throws IOException {
				return 0 < remaining-- ? is.read() : -1;
			}

			public int read(byte[] bytes, int offset, int length) throws IOException {
				int result;

				if (0 < remaining) {
					result = is.read(bytes, offset, min(length, remaining));

					if (0 <= result)
						remaining -= result;
				} else
					result = -1;

				return result;
			}
		};
	}

}
