package suite.net.nio;

import static suite.util.Friends.rethrow;

import java.io.Closeable;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;

import suite.cfg.Defaults;
import suite.net.ThreadService;
import suite.net.nio.NioChannelFactory.NioChannel;
import suite.object.Object_;
import suite.os.LogUtil;
import suite.primitive.Bytes;
import suite.streamlet.FunUtil.Iterate;
import suite.streamlet.FunUtil.Source;

public class NioDispatcherImpl<C extends NioChannel> implements NioDispatcher<C> {

	private Source<C> channelSource;
	private Selector selector = Selector.open();
	private ThreadService threadService = new ThreadService(this::serve);

	public NioDispatcherImpl(Source<C> channelSource) throws IOException {
		this.channelSource = channelSource;
	}

	@Override
	public void start() {
		threadService.start();
	}

	@Override
	public void stop() {
		threadService.stop();
	}

	/**
	 * Establishes connection to a host actively.
	 */
	@Override
	public C connect(InetSocketAddress address) throws IOException {
		var cl = channelSource.source();
		reconnect(cl, address);
		return cl;
	}

	/**
	 * Re-establishes connection using specified listener, if closed or dropped.
	 */
	@Override
	public void reconnect(NioChannel channel, InetSocketAddress address) throws IOException {
		var sc = SocketChannel.open();
		sc.configureBlocking(false);
		sc.connect(address);
		sc.register(selector, SelectionKey.OP_CONNECT, channel);

		wakeUpSelector();
	}

	/**
	 * Ends connection.
	 */
	@Override
	public void disconnect(NioChannel channel) throws IOException {
		for (var key : selector.keys())
			if (key.attachment() == channel)
				key.channel().close();
	}

	/**
	 * Waits for incoming connections.
	 *
	 * @return event for switching off the server.
	 */
	@Override
	public Closeable listen(int port) throws IOException {
		var ssc = ServerSocketChannel.open();
		ssc.configureBlocking(false);
		ssc.socket().bind(new InetSocketAddress(port));
		ssc.register(selector, SelectionKey.OP_ACCEPT);

		wakeUpSelector();
		return () -> Object_.closeQuietly(ssc);
	}

	private void serve() throws IOException {
		try (var started = threadService.started()) {
			while (threadService.isRunning()) {

				// unfortunately Selector.wakeup() does not work on my Linux
				// machines. Thus we specify a time out to allow the selector
				// freed out temporarily; otherwise the register() methods in
				// other threads might block forever.
				selector.select(500);

				// this seems to allow other threads to gain access. Not exactly
				// the behavior as documented in NIO, but anyway.
				selector.wakeup();

				var iter = selector.selectedKeys().iterator();

				while (iter.hasNext()) {
					var key = iter.next();
					iter.remove();

					try {
						processSelectedKey(key);
					} catch (Exception ex) {
						LogUtil.error(ex);
					}
				}
			}
		}

		selector.close();
	}

	private void processSelectedKey(SelectionKey key) throws IOException {
		// logUtil.info("KEY", dumpKey(key));

		var buffer = new byte[Defaults.bufferSize];
		var attachment = key.attachment();
		var sc0 = key.channel();
		var ops = key.readyOps();

		if ((ops & SelectionKey.OP_ACCEPT) != 0) {
			var channel = channelSource.source();
			var sc = ((ServerSocketChannel) sc0).accept().socket().getChannel();
			sc.configureBlocking(false);
			sc.register(selector, SelectionKey.OP_READ, channel);
			channel.onConnected.fire(newSender(sc));
		}

		if ((ops & ~SelectionKey.OP_ACCEPT) != 0)
			synchronized (attachment) {
				@SuppressWarnings("unchecked")
				var channel = (C) attachment;
				var sc1 = (SocketChannel) sc0;

				if ((ops & SelectionKey.OP_CONNECT) != 0) {
					sc1.finishConnect();
					key.interestOps(SelectionKey.OP_READ);
					channel.onConnected.fire(newSender(sc1));
				}

				if ((ops & SelectionKey.OP_READ) != 0) {
					var n = sc1.read(ByteBuffer.wrap(buffer));
					if (0 <= n)
						channel.onReceive.fire(Bytes.of(buffer, 0, n));
					else {
						channel.onConnected.fire(null);
						sc1.close();
					}
				}

				if ((ops & SelectionKey.OP_WRITE) != 0)
					channel.onTrySend.fire(Boolean.TRUE);
			}
	}

	private Iterate<Bytes> newSender(SocketChannel sc) {
		return in -> {

			// try to send immediately. If cannot sent all, wait for the
			// writable event (and send again at that moment).
			var bytes = in.toArray();
			var sent = rethrow(() -> sc.write(ByteBuffer.wrap(bytes)));
			var out = in.range(sent);
			var ops = SelectionKey.OP_READ | (!out.isEmpty() ? SelectionKey.OP_WRITE : 0);
			var key = sc.keyFor(selector);

			if (key != null && key.interestOps() != ops)
				key.interestOps(ops);

			wakeUpSelector();
			return out;
		};
	}

	private void wakeUpSelector() {
		// selector.wakeup(); // not working in my Linux machines
	}

}
