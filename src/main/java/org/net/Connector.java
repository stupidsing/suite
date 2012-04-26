package org.net;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.concurrent.ThreadPoolExecutor;

import org.util.LogUtil;
import org.util.Util;

public class Connector {

	public void connect() throws UnknownHostException, IOException {
		Socket socket = new Socket("wwww.google.com", 80);
		InputStream is = socket.getInputStream();
		OutputStream os = socket.getOutputStream();
		BufferedReader reader = new BufferedReader(new InputStreamReader(is));
		PrintWriter writer = new PrintWriter(os);

		try {
			writer.print("GET /\r\n\r\n");
			while (reader.ready())
				System.out.println((char) reader.read());
		} finally {
			Util.closeQuietly(reader);
			Util.closeQuietly(writer);
		}
	}

	public void listen() throws IOException {
		ServerSocket server = new ServerSocket(5151);
		ThreadPoolExecutor executor = Util.createExecutor();

		while (true) {
			final Socket socket = server.accept();

			executor.execute(new Runnable() {
				public void run() {
					BufferedReader reader = null;
					PrintWriter writer = null;

					try {
						OutputStream os = socket.getOutputStream();
						InputStream is = socket.getInputStream();
						reader = new BufferedReader(new InputStreamReader(is));
						writer = new PrintWriter(os);

						writer.println("Hello World");
					} catch (IOException ex) {
						LogUtil.error(Connector.this.getClass(), ex);
					} finally {
						Util.closeQuietly(reader);
						Util.closeQuietly(writer);
						Util.closeQuietly(socket);
					}
				}
			});
		}
	}

}
