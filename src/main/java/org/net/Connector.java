package org.net;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.util.LogUtil;
import org.util.Util;

public class Connector {

	private final static int BUFFERSIZE = 4096;

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
		ThreadPoolExecutor executor = new ThreadPoolExecutor(8, 32 //
				, 10, TimeUnit.SECONDS //
				, new ArrayBlockingQueue<Runnable>(256));

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

	public void nioServer() throws IOException {
		Selector selector = Selector.open();

		InetAddress localHost = InetAddress.getByName("0.0.0.0");
		ServerSocketChannel ssc = ServerSocketChannel.open();
		ssc.configureBlocking(false);
		ssc.socket().bind(new InetSocketAddress(localHost, 5151));
		ssc.register(selector, SelectionKey.OP_ACCEPT);

		byte[] buffer = new byte[BUFFERSIZE];

		boolean isWritten = false;

		while (true) {
			selector.select();
			Iterator<SelectionKey> iter = selector.selectedKeys().iterator();

			while (iter.hasNext()) {
				SelectionKey key = iter.next();
				iter.remove();

				SelectableChannel channel = key.channel();

				if (key.isAcceptable()) {
					ServerSocketChannel ssc1 = (ServerSocketChannel) channel;
					SocketChannel sc = ssc1.accept().socket().getChannel();
					sc.configureBlocking(false);
					sc.register(selector, SelectionKey.OP_READ
							| SelectionKey.OP_WRITE);
				} else if (key.isReadable()) {
					SocketChannel sc1 = (SocketChannel) channel;

					int n = sc1.read(ByteBuffer.wrap(buffer));
					if (n >= 0)
						System.out.print("READ: " + new String(buffer, 0, n));
					else {
						System.out.println("CLOSED");
						sc1.close();
					}
				} else if (key.isWritable()) {
					String str = "hello\n";
					System.arraycopy(str.getBytes(), 0, buffer, 0, str.length());

					SocketChannel sc1 = (SocketChannel) channel;
					if (!isWritten) {
						sc1.write(ByteBuffer.wrap(buffer));
						isWritten = true;
					}
				}
			}
		}
	}

}
