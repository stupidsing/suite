package suite.sample;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.Reader;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.Charset;
import java.util.concurrent.ThreadPoolExecutor;

import suite.util.FileUtil;
import suite.util.LogUtil;
import suite.util.Util;

public class Connector {

	public void connect() throws IOException {
		Charset charset = FileUtil.charset;

		try (Socket socket = new Socket("wwww.google.com", 80);
				InputStream is = socket.getInputStream();
				OutputStream os = socket.getOutputStream();
				Reader reader = new BufferedReader(new InputStreamReader(is, charset));
				PrintWriter writer = new PrintWriter(os)) {
			writer.print("GET /\r\n\r\n");
			while (reader.ready())
				System.out.println((char) reader.read());
		}
	}

	public void listen() throws IOException {
		ThreadPoolExecutor executor = Util.createExecutor();

		try (ServerSocket server = new ServerSocket(5151)) {
			while (true) {
				final Socket socket = server.accept();

				executor.execute(new Runnable() {
					public void run() {
						try (OutputStream os = socket.getOutputStream();
								InputStream is = socket.getInputStream();
								Reader isr = new BufferedReader(new InputStreamReader(is));
								PrintWriter writer = new PrintWriter(os)) {
							writer.println("Hello World");
						} catch (IOException ex) {
							LogUtil.error(ex);
						}
					}
				});
			}
		} finally {
			executor.shutdown();
		}
	}

}
