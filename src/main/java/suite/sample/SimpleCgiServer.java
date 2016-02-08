package suite.sample;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.IntBuffer;
import java.util.HashMap;
import java.util.Map;

import suite.os.FileUtil;
import suite.os.SocketUtil;
import suite.util.To;

public class SimpleCgiServer {

	public interface Handler {
		public void handle(Map<String, String> headers, OutputStream os) throws IOException;
	}

	public static void main(String args[]) throws IOException {
		new SimpleCgiServer().run((headers, os) -> {
			OutputStreamWriter writer = new OutputStreamWriter(os, FileUtil.charset);
			writer.write("<html>" + headers + "</html>");
		});
	}

	private void run(Handler handler) throws IOException {
		new SocketUtil().listenIo(4000, (is, os) -> {
			Map<String, String> headers = readHeaders(is);

			os.write(("Status: 200 OK\r\n" //
					+ "Content-Type: text/html\r\n" //
					+ "\r\n").getBytes(FileUtil.charset));

			handler.handle(headers, os);
		});
	}

	private Map<String, String> readHeaders(InputStream sis) throws IOException {
		String header = readNetstring(sis);

		IntBuffer zeroPositions = IntBuffer.allocate(256);

		for (int i = 0; i < header.length(); i++)
			if (header.charAt(i) == 0)
				zeroPositions.put(i);

		zeroPositions.flip();

		Map<String, String> headers = new HashMap<>();
		int start = 0;
		int i = 0;

		while (i < zeroPositions.limit() - 1) {
			int z0 = zeroPositions.get(i++);
			int z1 = zeroPositions.get(i++);
			String key = header.substring(start, z0);
			String value = header.substring(z0 + 1, z1);
			start = z1 + 1;
			headers.put(key, value);
		}

		return headers;
	}

	private String readNetstring(InputStream sis) throws IOException {
		int length = 0;
		int c;

		while (0 <= (c = sis.read()) && c != ':' && length < 65536)
			if ('0' <= c && c <= '9')
				length = length * 10 + c - '0';
			else
				throw new RuntimeException("Invalid netstring length");

		if (c != ':')
			throw new RuntimeException("Netstring length not ended with ':'");

		int nBytesRead = 0;
		byte bytes[] = new byte[length];
		while (nBytesRead < length)
			nBytesRead += sis.read(bytes);

		return To.string(bytes);
	}

}
