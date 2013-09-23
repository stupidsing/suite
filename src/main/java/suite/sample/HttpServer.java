package suite.sample;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.util.HashMap;
import java.util.Map;

import suite.util.FileUtil;
import suite.util.Pair;
import suite.util.SocketUtil;
import suite.util.SocketUtil.Io;
import suite.util.Util;

public class HttpServer {

	public interface Handler {
		public void handle(String method //
				, String server //
				, String path //
				, String query //
				, Map<String, String> headers //
				, OutputStream os) throws IOException;
	}

	public static void main(String args[]) throws IOException {
		new HttpServer().run(new Handler() {
			public void handle(String method //
					, String server //
					, String path //
					, String query //
					, Map<String, String> headers //
					, OutputStream os) throws IOException {
				try (Writer writer = new OutputStreamWriter(os, FileUtil.charset)) {
					writer.write("<html>" + headers + "</html>");
				}
			}
		});
	}

	private void run(final Handler handler) throws IOException {
		SocketUtil.listen(80, new Io() {
			public void serve(InputStream is, OutputStream os) throws IOException {
				BufferedReader br = new BufferedReader(new InputStreamReader(is, FileUtil.charset));

				while (br.ready()) {
					String line, ls[];

					line = readLine(br);
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

					String path, query;
					int pos = pqs.indexOf('?');

					if (pos >= 0) {
						path = pqs.substring(0, pos);
						query = pqs.substring(pos + 1);
					} else {
						path = pqs;
						query = null;
					}

					if (!Util.equals(protocol, "HTTP/1.1"))
						throw new RuntimeException("Only HTTP/1.1 is supported");

					Map<String, String> headers = new HashMap<>();

					while (!(line = readLine(br)).isEmpty()) {
						Pair<String, String> pair = Util.split2(line, ":");
						headers.put(pair.t0, pair.t1);
					}

					ByteArrayOutputStream baos = new ByteArrayOutputStream();
					handler.handle(method, server, path, query, headers, baos);

					String responseHeader = "HTTP/1.1 200 OK\r\n" //
							+ "Content-Length: " + baos.size() + "\r\n" //
							+ "Content-Type: text/html; charset=UTF-8\r\n" //
							+ "\r\n";

					os.write(responseHeader.getBytes(FileUtil.charset));
					os.write(baos.toByteArray());
				}
			}
		});
	}

	/**
	 * Reads a line from a reader with a maximum line length limit. Removes
	 * carriage return if it is DOS-mode line feed (CR-LF).
	 */
	private String readLine(Reader reader) throws IOException {
		StringBuilder sb = new StringBuilder();
		int c;

		while ((c = reader.read()) != 10) {
			sb.append((char) c);
			if (sb.length() > 65536)
				throw new RuntimeException("Line too long");
		}

		if (sb.charAt(sb.length() - 1) == 13)
			sb.deleteCharAt(sb.length() - 1);

		return sb.toString();
	}

}
