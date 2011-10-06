package org.net;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;

import org.net.NioDispatcher.ChannelListener;
import org.util.IoUtil;
import org.util.LogUtil;
import org.util.Util.Event;
import org.util.Util.IoProcess;

public class NioDispatcher<CL extends ChannelListener> extends ThreadedService {

	public interface ChannelListenerFactory<CL> {
		public CL create();
	}

	public interface ChannelListener {
		public void onConnected();

		public void onReceive(String message);

		public void onClose() throws IOException;

		public void trySend() throws IOException;

		/**
		 * The event would be invoked when the channel wants to send anything,
		 * i.e. getMessageToSend() would return data.
		 */
		public void setTrySendDelegate(
				IoProcess<String, String, IOException> sender);
	}

	private final static int BUFFERSIZE = 4096;

	private ChannelListenerFactory<CL> factory;

	private Selector selector = Selector.open();

	public NioDispatcher(ChannelListenerFactory<CL> factory) throws IOException {
		this.factory = factory;
	}

	/**
	 * Establishes connection to other host actively.
	 */
	public CL connect(InetSocketAddress address) throws IOException {
		CL listener = factory.create();
		reconnect(listener, address);
		return listener;
	}

	/**
	 * Re-establishes connection using specified listener, if closed or dropped.
	 */
	public void reconnect(ChannelListener listener, InetSocketAddress address)
			throws IOException {
		SocketChannel channel = SocketChannel.open();
		channel.configureBlocking(false);
		channel.connect(address);
		channel.register(selector, SelectionKey.OP_CONNECT, listener);

		wakeUpSelector();
	}

	/**
	 * Ends connection.
	 */
	public void disconnect(CL listener) throws IOException {
		for (SelectionKey key : selector.keys())
			if (key.attachment() == listener)
				key.channel().close();
	}

	/**
	 * Waits for incoming connections.
	 * 
	 * @return event for switching off the server.
	 */
	public Event listen(int port) throws IOException {
		final ServerSocketChannel ssc = ServerSocketChannel.open();
		ssc.configureBlocking(false);
		ssc.socket().bind(new InetSocketAddress(port));
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

	@Override
	protected void serve() throws IOException {
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

				try {
					processSelectedKey(key);
				} catch (Exception ex) {
					LogUtil.error(getClass(), ex);
				}
			}
		}

		setStarted(false);
		selector.close();
	}

	private void processSelectedKey(SelectionKey key) throws IOException {
		// LogUtil.info("KEY", dumpKey(key));

		byte buffer[] = new byte[BUFFERSIZE];
		SelectableChannel channel = key.channel();
		Object attachment = key.attachment();

		if (key.isAcceptable()) {
			final ChannelListener listener = factory.create();
			ServerSocketChannel ssc = (ServerSocketChannel) channel;
			Socket socket = ssc.accept().socket();
			final SocketChannel sc = socket.getChannel();

			sc.configureBlocking(false);
			key = sc.register(selector, SelectionKey.OP_READ, listener);

			listener.setTrySendDelegate(createTrySendDelegate(sc));
			listener.onConnected();
		} else
			synchronized (attachment) {
				ChannelListener listener = (ChannelListener) attachment;
				SocketChannel sc = (SocketChannel) channel;

				if (key.isConnectable()) {
					sc.finishConnect();

					key.interestOps(SelectionKey.OP_READ);
					listener.setTrySendDelegate(createTrySendDelegate(sc));
					listener.onConnected();
				} else if (key.isReadable()) {
					int n = sc.read(ByteBuffer.wrap(buffer));

					if (n >= 0) {
						String m = new String(buffer, 0, n, IoUtil.CHARSET);
						listener.onReceive(m);
					} else {
						listener.onClose();
						sc.close();
					}
				} else if (key.isWritable())
					listener.trySend();
			}
	}

	private IoProcess<String, String, IOException> createTrySendDelegate(
			final SocketChannel channel) {
		return new IoProcess<String, String, IOException>() {
			public String perform(String in) throws IOException {

				// Try to send immediately. If cannot sent all, wait for the
				// writable event (and send again at that moment).
				byte bytes[] = in.getBytes(IoUtil.CHARSET);
				int sent = channel.write(ByteBuffer.wrap(bytes));

				String out = in.substring(sent);

				int ops;
				if (!out.isEmpty())
					ops = SelectionKey.OP_READ | SelectionKey.OP_WRITE;
				else
					ops = SelectionKey.OP_READ;

				SelectionKey key = channel.keyFor(selector);
				if (key != null && key.interestOps() != ops)
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
