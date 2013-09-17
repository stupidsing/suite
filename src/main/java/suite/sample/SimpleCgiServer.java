package suite.sample;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.IntBuffer;
import java.util.HashMap;
import java.util.Map;

import suite.util.FileUtil;
import suite.util.LogUtil;
import suite.util.Util;

public class SimpleCgiServer {

	public static void main(String args[]) throws IOException {
		run();
	}

	private static void run() throws IOException {
		try (ServerSocket serverSocket = new ServerSocket(4000)) {
			while (true)
				new SimpleCgiHandlerThread(serverSocket.accept()).start();
		}
	}

	protected void serve(Map<String, String> headers, OutputStream os) throws IOException {
		headers.getClass();
		os.write("<html></html>".getBytes(FileUtil.charset));
	}

	public class SimpleCgiHandlerThread extends Thread {
		private Socket socket;

		private SimpleCgiHandlerThread(Socket socket) {
			this.socket = socket;
		}

		public void run() {
			try (InputStream sis = socket.getInputStream(); OutputStream sos = socket.getOutputStream()) {
				Map<String, String> headers = readHeaders(sis);

				sos.write(("Status: 200 OK\r\n" //
						+ "Content-Type: text/html\r\n" //
						+ "\r\n").getBytes(FileUtil.charset));

				serve(headers, sos);
			} catch (Exception ex) {
				LogUtil.error(ex);
			} finally {
				Util.closeQuietly(socket);
			}
		}
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

		while ((c = sis.read()) >= 0 && c != ':' && length < 65536)
			if (c >= '0' && c <= '9')
				length = length * 10 + c - '0';
			else
				throw new RuntimeException("Invalid netstring length");

		if (c != ':')
			throw new RuntimeException("Netstring length not ended with ':'");

		int nBytesRead = 0;
		byte bytes[] = new byte[length];
		while (nBytesRead < length)
			nBytesRead += sis.read(bytes);

		return new String(bytes, FileUtil.charset);
	}

}
