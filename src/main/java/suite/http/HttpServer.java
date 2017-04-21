package suite.http;

import java.io.IOException;
import java.io.InputStream;
import java.net.URLDecoder;
import java.util.HashMap;
import java.util.Map;

import suite.Constants;
import suite.adt.Pair;
import suite.os.LogUtil;
import suite.os.SocketUtil;
import suite.util.Copy;
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
public class HttpServer {

	public void run(HttpHandler handler) {
		try {
			run_(handler);
		} catch (IOException ex) {
			throw new RuntimeException(ex);
		}
	}

	private void run_(HttpHandler handler) throws IOException {
		new SocketUtil().listenIo(8051, (is, os) -> {
			String line, ls[];

			ls = Util.readLine(is).split(" ");
			String method = ls[0], url = ls[1], protocol = ls[2];
			String server, pqs;

			Pair<String, String> pp = Util.split2(url, "://");

			if (Util.isNotBlank(pp.t1)) {
				Pair<String, String> sp = Util.split2(pp.t1, "/");
				server = sp.t0;
				pqs = sp.t1;
			} else {
				server = "";
				pqs = url;
			}

			Pair<String, String> pq = Util.split2(pqs, "?");
			String path = pq.t0;
			String query = pq.t1;

			String path1 = path.startsWith("/") ? path : "/" + path;
			String path2 = URLDecoder.decode(path1, "UTF-8");

			if (!Util.stringEquals(protocol, "HTTP/1.1"))
				throw new RuntimeException("Only HTTP/1.1 is supported");

			Map<String, String> requestHeaders = new HashMap<>();

			while (!(line = Util.readLine(is)).isEmpty()) {
				Pair<String, String> pair = Util.split2(line, ":");
				requestHeaders.put(pair.t0, pair.t1);
			}

			String cls = requestHeaders.get("Content-Length");
			int contentLength = cls != null ? Integer.parseInt(cls) : 0;
			InputStream cis = sizeLimitedInputStream(is, contentLength);

			HttpRequest request = new HttpRequest(method, server, path2, query, requestHeaders, cis);
			HttpResponse response = null;

			try {
				response = handler.handle(request);
			} catch (Exception ex) {
				LogUtil.error(ex);
				response = HttpResponse.of(HttpResponse.HTTP500);
			} finally {
				LogUtil.info(request.getLogString() + " " + response.getLogString());
			}

			StringBuilder sb = new StringBuilder();

			sb.append("HTTP/1.1 " + response.status + "\r\n");
			for (Pair<String, String> entry : response.headers)
				sb.append(entry.t0 + ": " + entry.t1 + "\r\n");
			sb.append("\r\n");

			os.write(sb.toString().getBytes(Constants.charset));
			Copy.stream(To.inputStream(response.out), os);
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
					result = is.read(bytes, offset, Math.min(length, remaining));

					if (0 <= result)
						remaining -= result;
				} else
					result = -1;

				return result;
			}
		};
	}

}
