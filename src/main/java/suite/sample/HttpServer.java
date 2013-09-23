package suite.sample;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.net.URLDecoder;
import java.util.HashMap;
import java.util.Map;

import suite.util.FileUtil;
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
		public void handle(String method //
				, String server //
				, String path //
				, String query //
				, Map<String, String> headers //
				, InputStream is //
				, OutputStream os) throws IOException;
	}

	public interface QueryAttrHandler {
		public void handle(String method //
				, String server //
				, String path //
				, Map<String, String> attrs //
				, Map<String, String> headers //
				, InputStream is //
				, OutputStream os) throws IOException;
	}

	public static void main(String args[]) throws IOException {
		new HttpServer().run(new QueryAttrHandler() {
			public void handle(String method //
					, String server //
					, String path //
					, Map<String, String> attrs //
					, Map<String, String> headers //
					, InputStream is //
					, OutputStream os) throws IOException {
				try (Writer writer = new OutputStreamWriter(os, FileUtil.charset)) {
					String s = "<html>" //
							+ "<br/>method = " + method //
							+ "<br/>server = " + server //
							+ "<br/>path = " + path //
							+ "<br/>attrs = " + attrs //
							+ "<br/>headers = " + headers //
							+ "</html>";
					writer.write(s);
				}
			}
		});
	}

	private void run(final QueryAttrHandler queryAttrHandler) throws IOException {
		run(new Handler() {
			public void handle(String method //
					, String server //
					, String path //
					, String query //
					, Map<String, String> headers //
					, InputStream is //
					, OutputStream os) throws IOException {
				String qs[] = query != null ? query.split("&") : new String[0];
				Map<String, String> attrs = new HashMap<>();

				for (String q : qs) {
					Pair<String, String> pair = Util.split2(q, "=");
					attrs.put(pair.t0, URLDecoder.decode(pair.t1, "UTF-8"));
				}

				queryAttrHandler.handle(method, server, path, attrs, headers, is, os);
			}
		});
	}

	private void run(final Handler handler) throws IOException {
		SocketUtil.listen(8051, new Io() {
			public void serve(InputStream is, OutputStream os) throws IOException {
				String line, ls[];

				line = readLine(is);
				ls = line.split(" ");
				String method = ls[0], url = ls[1], protocol = ls[2];
				String server, pqs;

				if (url.startsWith("http://")) {
					String url1 = url.substring(7);
					int pos = url1.indexOf(':');
					server = url1.substring(0, pos);
					pqs = url1.substring(pos);
				} else {
					server = "";
					pqs = url;
				}

				int pos = pqs.indexOf('?');
				String path, query;

				if (pos >= 0) {
					path = pqs.substring(0, pos);
					query = pqs.substring(pos + 1);
				} else {
					path = pqs;
					query = null;
				}

				String path1 = URLDecoder.decode(path, "UTF-8");

				if (!Util.equals(protocol, "HTTP/1.1"))
					throw new RuntimeException("Only HTTP/1.1 is supported");

				Map<String, String> headers = new HashMap<>();

				while (!(line = readLine(is)).isEmpty()) {
					Pair<String, String> pair = Util.split2(line, ":");
					headers.put(pair.t0, pair.t1);
				}

				String cls = headers.get("Content-Length");
				int contentLength = cls != null ? Integer.valueOf(cls) : 0;
				InputStream cis = sizeLimitedInputStream(is, contentLength);

				ByteArrayOutputStream baos = new ByteArrayOutputStream();

				handler.handle(method, server, path1, query, headers, cis, baos);

				String responseHeader = "HTTP/1.1 200 OK\r\n" //
						+ "Content-Length: " + baos.size() + "\r\n" //
						+ "Content-Type: text/html; charset=UTF-8\r\n" //
						+ "\r\n";

				os.write(responseHeader.getBytes(FileUtil.charset));
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
				int nBytesRead = is.read(bytes, offset, Math.min(length, remaining));
				remaining -= nBytesRead;
				return nBytesRead;
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
