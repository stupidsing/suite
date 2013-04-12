package org.net;

import java.io.Closeable;
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
import java.util.Set;

import org.net.ChannelListeners.ChannelListener;
import org.util.LogUtil;
import org.util.Util.FunEx;
import org.util.Util.Source;

public class NioDispatcher<CL extends ChannelListener> extends ThreadedService {

	private static final int bufferSize = 4096;

	private Source<CL> channelListenerSource;
	private Selector selector = Selector.open();

	public NioDispatcher(Source<CL> channelListenerSource) throws IOException {
		this.channelListenerSource = channelListenerSource;
	}

	/**
	 * Establishes connection to other host actively.
	 */
	public CL connect(InetSocketAddress address) throws IOException {
		CL listener = channelListenerSource.apply(null);
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
	public Closeable listen(int port) throws IOException {
		final ServerSocketChannel ssc = ServerSocketChannel.open();
		ssc.configureBlocking(false);
		ssc.socket().bind(new InetSocketAddress(port));
		ssc.register(selector, SelectionKey.OP_ACCEPT);

		wakeUpSelector();

		return new Closeable() {
			public void close() {
				try {
					ssc.close();
				} catch (IOException ex) {
					LogUtil.error(getClass(), ex);
				}
			}
		};
	}

	@Override
	protected void serve() throws IOException {
		try (Closeable started = started()) {
			while (running) {

				// Unfortunately Selector.wakeup() does not work on my Linux
				// machines. Thus we specify a time out to allow the selector
				// freed out temporarily; otherwise the register() methods in
				// other threads might block forever.
				selector.select(500);

				// This seems to allow other threads to gain access. Not exactly
				// the behavior as documented in NIO, but anyway.
				selector.wakeup();

				Set<SelectionKey> selectedKeys = selector.selectedKeys();
				Iterator<SelectionKey> iter = selectedKeys.iterator();

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
		}

		selector.close();
	}

	private void processSelectedKey(SelectionKey key) throws IOException {
		// LogUtil.info("KEY", dumpKey(key));

		byte buffer[] = new byte[bufferSize];
		SelectableChannel channel = key.channel();
		Object attachment = key.attachment();

		if (key.isAcceptable()) {
			final ChannelListener cl = channelListenerSource.apply(null);
			ServerSocketChannel ssc = (ServerSocketChannel) channel;
			Socket socket = ssc.accept().socket();
			final SocketChannel sc = socket.getChannel();

			sc.configureBlocking(false);
			key = sc.register(selector, SelectionKey.OP_READ, cl);

			cl.setTrySendDelegate(createTrySendDelegate(sc));
			cl.onConnected();
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

					if (n >= 0)
						listener.onReceive(new Bytes(buffer, 0, n));
					else {
						listener.onClose();
						sc.close();
					}
				} else if (key.isWritable())
					listener.trySend();
			}
	}

	private FunEx<Bytes, Bytes, IOException> createTrySendDelegate(
			final SocketChannel channel) {
		return new FunEx<Bytes, Bytes, IOException>() {
			public Bytes apply(Bytes in) throws IOException {

				// Try to send immediately. If cannot sent all, wait for the
				// writable event (and send again at that moment).
				byte bytes[] = in.getBytes();
				int sent = channel.write(ByteBuffer.wrap(bytes));

				Bytes out = in.subbytes(sent);

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
