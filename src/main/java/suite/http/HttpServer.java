package suite.http;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLDecoder;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import suite.Constants;
import suite.adt.Pair;
import suite.os.LogUtil;
import suite.os.SocketUtil;
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

	public void run(HttpHandler handler) throws IOException {
		new SocketUtil().listenIo(8051, (is, os) -> {
			HashMap<String, String> responseHeaders = new HashMap<>();
			ByteArrayOutputStream baos = new ByteArrayOutputStream();

			String line, ls[];

			line = Util.readLine(is);
			ls = line.split(" ");
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
			HttpResponse response = new HttpResponse("200 OK", responseHeaders, baos);

			try {
				handler.handle(request, response);
			} catch (Exception ex) {
				LogUtil.error(ex);
				response.setStatus("500 Internal server error");
			} finally {
				LogUtil.info(request.getLogString() + " " + response.getLogString());
			}

			responseHeaders.put("Content-Length", Integer.toString(baos.size()));
			responseHeaders.put("Content-Type", "text/html; charset=UTF-8");

			StringBuilder sb = new StringBuilder();

			sb.append("HTTP/1.1 " + response.getStatus() + "\r\n");
			for (Entry<String, String> entry : responseHeaders.entrySet())
				sb.append(entry.getKey() + ": " + entry.getValue() + "\r\n");
			sb.append("\r\n");

			os.write(sb.toString().getBytes(Constants.charset));
			os.write(baos.toByteArray());
		});
	}

	private InputStream sizeLimitedInputStream(InputStream is, int size) {
		return new BasicInputStream(is) {
			private int remaining = size;

			public int read() throws IOException {
				return 0 < remaining-- ? is.read() : -1;
			}

			public int read(byte bytes[], int offset, int length) throws IOException {
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
