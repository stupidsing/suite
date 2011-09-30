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
import org.util.Util.Transformer;

public class NioServer<CL extends ChannelListener> {

	public interface ChannelListenerFactory<CL> {
		public CL create();
	}

	public interface ChannelListener {
		public void onConnected();

		public void onReceive(String message);

		public void onClose();

		public void trySend();

		/**
		 * The event would be invoked when the channel wants to send anything,
		 * i.e. getMessageToSend() would return data.
		 */
		public void setSendDelegate(
				Transformer<String, String, RuntimeException> sender);
	}

	public interface SendNotifier {
		public void goingToSend();
	}

	private final static int BUFFERSIZE = 4096;

	private ChannelListenerFactory<CL> factory;
	private boolean started;

	private Selector selector = Selector.open();
	private Thread eventLoopThread;
	private volatile boolean running = false;

	public NioServer(ChannelListenerFactory<CL> factory) throws IOException {
		this.factory = factory;
	}

	public synchronized void spawn() {
		running = true;

		eventLoopThread = new Thread() {
			public void run() {
				try {
					serve();
				} catch (IOException ex) {
					LogUtil.error(getClass(), ex);
				}
			}
		};

		eventLoopThread.start();

		while (started != true)
			Util.wait(this);
	}

	public synchronized void unspawn() {
		running = false;

		eventLoopThread.interrupt();

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

		wakeUpSelector();
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

		wakeUpSelector();

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

		while (running) {

			// Unfortunately Selector.wakeup() does not work on my Linux
			// machines. Thus we specify a time out to allow the selector freed
			// out temporarily; otherwise the register() methods in other
			// threads might block forever.
			selector.select(500);

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
		// LogUtil.info("KEY", dumpKey(key));

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

			listener.setSendDelegate(createEventForSend(sc));
			listener.onConnected();
		} else
			synchronized (attachment) {
				ChannelListener listener = (ChannelListener) attachment;
				SocketChannel sc = (SocketChannel) channel;

				if (key.isConnectable()) {
					sc.finishConnect();

					key.interestOps(SelectionKey.OP_READ);
					listener.setSendDelegate(createEventForSend(sc));
					listener.onConnected();
				} else if (key.isReadable()) {
					int n = sc.read(ByteBuffer.wrap(buffer));

					if (n >= 0)
						listener.onReceive(new String(buffer, 0, n));
					else {
						listener.onClose();
						sc.close();
					}
				} else if (key.isWritable())
					listener.trySend();
			}
	}

	private Transformer<String, String, RuntimeException> createEventForSend(
			final SocketChannel channel) {
		return new Transformer<String, String, RuntimeException>() {
			public String perform(String in) {
				int sent = 0;

				try {
					sent = channel.write(ByteBuffer.wrap(in.getBytes()));
				} catch (IOException ex) {
					LogUtil.error(getClass(), ex);
				}

				String out = in.substring(sent);

				int ops;
				if (!out.isEmpty())
					ops = SelectionKey.OP_READ | SelectionKey.OP_WRITE;
				else
					ops = SelectionKey.OP_READ;

				SelectionKey key = channel.keyFor(selector);
				if (key.interestOps() != ops)
					key.interestOps(ops);

				wakeUpSelector();
				return out;
			}
		};
	}

	private void wakeUpSelector() {
		// selector.wakeup(); // Not working in my Linux machines
	}

	@SuppressWarnings("unused")
	private String dumpKey(SelectionKey key) {
		return (key.isAcceptable() ? "ACCEPTABLE " : "") //
				+ (key.isConnectable() ? "CONNECTABLE " : "") //
				+ (key.isReadable() ? "READABLE " : "") //
				+ (key.isWritable() ? "WRITABLE " : "") //
				+ key.channel() + " " + key.selector().toString() //
		;
	}

}
