package org.net;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.Reader;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.charset.Charset;
import java.util.concurrent.ThreadPoolExecutor;

import org.util.IoUtil;
import org.util.LogUtil;
import org.util.Util;

public class Connector {

	public void connect() throws UnknownHostException, IOException {
		Charset charset = IoUtil.charset;

		try (Socket socket = new Socket("wwww.google.com", 80);
				InputStream is = socket.getInputStream();
				OutputStream os = socket.getOutputStream();
				Reader isr = new InputStreamReader(is, charset);
				Reader reader = new BufferedReader(isr);
				PrintWriter writer = new PrintWriter(os);) {
			writer.print("GET /\r\n\r\n");
			while (reader.ready())
				System.out.println((char) reader.read());
		}
	}

	public void listen() throws IOException {
		try (ServerSocket server = new ServerSocket(5151);) {
			ThreadPoolExecutor executor = Util.createExecutor();

			while (true) {
				final Socket socket = server.accept();

				executor.execute(new Runnable() {
					public void run() {
						try (OutputStream os = socket.getOutputStream();
								InputStream is = socket.getInputStream();
								Reader isr = new InputStreamReader(is);
								Reader reader = new BufferedReader(isr);
								PrintWriter writer = new PrintWriter(os);) {
							writer.println("Hello World");
						} catch (IOException ex) {
							LogUtil.error(Connector.this.getClass(), ex);
						}
					}
				});
			}
		}
	}

}
