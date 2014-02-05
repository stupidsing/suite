package suite.http;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URLDecoder;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import suite.util.FileUtil;
import suite.util.LogUtil;
import suite.util.Pair;
import suite.util.SocketUtil;
import suite.util.SocketUtil.Io;
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

	public interface Handler {
		public void handle(HttpRequest request, HttpResponse response) throws IOException;
	}

	public void run(final Handler handler) throws IOException {
		SocketUtil.listen(8051, new Io() {
			public void serve(InputStream is, OutputStream os) throws IOException {
				HashMap<String, String> responseHeaders = new HashMap<String, String>();
				ByteArrayOutputStream baos = new ByteArrayOutputStream();

				String line, ls[];

				line = readLine(is);
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

				if (!Util.equals(protocol, "HTTP/1.1"))
					throw new RuntimeException("Only HTTP/1.1 is supported");

				Map<String, String> requestHeaders = new HashMap<>();

				while (!(line = readLine(is)).isEmpty()) {
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

				os.write(sb.toString().getBytes(FileUtil.charset));
				os.write(baos.toByteArray());
			}
		});
	}

	private InputStream sizeLimitedInputStream(final InputStream is, final int size) {
		return new BasicInputStream(is) {
			private int remaining = size;

			public int read() throws IOException {
				return remaining-- > 0 ? is.read() : -1;
			}

			public int read(byte bytes[], int offset, int length) throws IOException {
				int result;

				if (remaining > 0) {
					result = is.read(bytes, offset, Math.min(length, remaining));

					if (result >= 0)
						remaining -= result;
				} else
					result = -1;

				return result;
			}
		};
	}

	/**
	 * Reads a line from a stream with a maximum line length limit. Removes
	 * carriage return if it is DOS-mode line feed (CR-LF).
	 */
	private String readLine(InputStream is) throws IOException {
		StringBuilder sb = new StringBuilder();
		int c;

		while ((c = is.read()) != 10) {
			sb.append((char) c);
			if (sb.length() > 65536)
				throw new RuntimeException("Line too long");
		}

		int length = sb.length();

		if (sb.charAt(length - 1) == 13)
			sb.deleteCharAt(length - 1);

		return sb.toString();
	}

}
