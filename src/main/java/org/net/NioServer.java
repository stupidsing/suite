package org.net;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;

import org.net.NioServer.ChannelListener;
import org.util.LogUtil;
import org.util.Util;
import org.util.Util.Event;

public class NioServer<CL extends ChannelListener> {

	public interface ChannelListenerFactory<CL> {
		public CL create();
	}

	public interface ChannelListener {
		public void onConnected();

		public void onReceive(String message);

		public void onSent(int size);

		public void onClose();

		/**
		 * The event would be invoked when the channel wants to send anything,
		 * i.e. getMessageToSend() would return data.
		 */
		public void setEventForSend(Event event);

		public String getMessageToSend();
	}

	public interface SendNotifier {
		public void goingToSend();
	}

	private final static int BUFFERSIZE = 4096;

	private ChannelListenerFactory<CL> factory;
	private boolean started;

	private Selector selector = Selector.open();
	private Thread thread;
	private volatile boolean running = false;

	public NioServer(ChannelListenerFactory<CL> factory) throws IOException {
		this.factory = factory;
	}

	public synchronized void spawn() {
		running = true;

		thread = new Thread() {
			public void run() {
				try {
					serve();
				} catch (IOException ex) {
					LogUtil.error(getClass(), ex);
				}
			}
		};

		thread.start();

		while (started != true)
			Util.wait(this);
	}

	public synchronized void unspawn() {
		running = false;

		thread.interrupt();

		while (started != false)
			Util.wait(this);
	}

	/**
	 * Establishes connection to other host actively.
	 */
	public CL connect(InetAddress host, int port) throws IOException {
		SocketChannel channel = SocketChannel.open();
		channel.configureBlocking(false);
		channel.connect(new InetSocketAddress(host, port));

		CL listener = factory.create();
		channel.register(selector, SelectionKey.OP_CONNECT, listener);
		return listener;
	}

	/**
	 * Waits for incoming connections.
	 * 
	 * @return event for switching off the server.
	 */
	public Event listen(int port) throws IOException {
		InetAddress localHost = InetAddress.getByName("0.0.0.0");

		final ServerSocketChannel ssc = ServerSocketChannel.open();
		ssc.configureBlocking(false);
		ssc.socket().bind(new InetSocketAddress(localHost, port));
		ssc.register(selector, SelectionKey.OP_ACCEPT);

		return new Event() {
			public Void perform(Void i) {
				try {
					ssc.close();
				} catch (IOException ex) {
					LogUtil.error(getClass(), ex);
				}

				return null;
			}
		};
	}

	private void serve() throws IOException {
		setStarted(true);

		Util.sleep(2000);

		while (running) {
			selector.select();
			Iterator<SelectionKey> iter = selector.selectedKeys().iterator();

			while (iter.hasNext()) {
				SelectionKey key = iter.next();
				iter.remove();

				processSelectedKey(key);
			}
		}

		setStarted(false);
		selector.close();
	}

	private synchronized void setStarted(boolean isStarted) {
		started = isStarted;
		notify();
	}

	private void processSelectedKey(SelectionKey key) throws IOException {
		byte buffer[] = new byte[BUFFERSIZE];
		SelectableChannel channel = key.channel();
		Object attachment = key.attachment();

		if (key.isAcceptable()) {
			final ChannelListener listener = factory.create();
			ServerSocketChannel ssc1 = (ServerSocketChannel) channel;
			Socket socket = ssc1.accept().socket();
			final SocketChannel sc = socket.getChannel();

			sc.configureBlocking(false);
			key = sc.register(selector, SelectionKey.OP_READ, listener);

			listener.setEventForSend(createEventForSend(key));
			listener.onConnected();
		} else
			synchronized (attachment) {
				ChannelListener listener = (ChannelListener) attachment;
				SocketChannel sc = (SocketChannel) channel;

				if (key.isConnectable()) {
					sc.finishConnect();

					key.interestOps(SelectionKey.OP_READ);
					listener.setEventForSend(createEventForSend(key));
					listener.onConnected();
				} else if (key.isReadable()) {
					int n = sc.read(ByteBuffer.wrap(buffer));

					if (n >= 0)
						listener.onReceive(new String(buffer, 0, n));
					else {
						listener.onClose();
						sc.close();
					}
				} else if (key.isWritable()) {
					String m = listener.getMessageToSend();
					int sent = sc.write(ByteBuffer.wrap(m.getBytes()));

					key.interestOps(SelectionKey.OP_READ);
					listener.onSent(sent);
				}
			}
	}

	private Event createEventForSend(final SelectionKey key) {
		return new Event() {
			public Void perform(Void v) {
				key.interestOps(SelectionKey.OP_READ | SelectionKey.OP_WRITE);
				return null;
			}
		};
	}

}
