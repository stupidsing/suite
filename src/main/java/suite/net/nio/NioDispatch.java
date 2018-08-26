package suite.net.nio;

import static suite.util.Friends.fail;

import java.io.Closeable;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Map;
import java.util.WeakHashMap;

import suite.cfg.Defaults;
import suite.net.nio.NioplexFactory.Nioplex;
import suite.object.Object_;
import suite.os.LogUtil;
import suite.primitive.BooMutable;
import suite.primitive.Bytes;
import suite.primitive.Bytes.BytesBuilder;
import suite.primitive.IoSink;
import suite.streamlet.FunUtil.Iterate;
import suite.streamlet.FunUtil.Source;
import suite.streamlet.Signal;

public class NioDispatch implements Closeable {

	public final Signal<SocketChannel> onDisconnected = Signal.of();

	private boolean isRunning = true;
	private Selector selector = Selector.open();
	private Map<SelectableChannel, BytesBuilder> reads = new WeakHashMap<>();

	public NioDispatch() throws IOException {
	}

	@Override
	public void close() throws IOException {
		stop();
		selector.close();
	}

	public void stop() {
		isRunning = false;
	}

	public class LinkNioplex {
		public <NP extends Nioplex> void asyncNioplexConnect(InetSocketAddress address, NP np) throws IOException {
			asyncConnect(address, sc -> linkNioplex(np, sc));
		}

		public <NP extends Nioplex> void asyncNioplexListen(int port, Source<NP> source) throws IOException {
			asyncListen(port, sc -> linkNioplex(source.source(), sc));
		}

		private <NP extends Nioplex> void linkNioplex(NP np, SocketChannel sc) throws ClosedChannelException {
			IoSink<Bytes> rr = np.onReceive::fire;
			Runnable rw = () -> np.onTrySend.fire(true);
			var writePending = BooMutable.false_();
			var or = SelectionKey.OP_READ;
			var ow = SelectionKey.OP_WRITE;

			Runnable reg = () -> reg(sc, writePending.isTrue() ? or | ow : or);

			Iterate<Bytes> sender = bs -> {
				// return bs.range(rethrow(() -> sc.write(bs.toByteBuffer())));

				if (!writePending.isTrue())
					try {
						asyncWriteAll(sc, bs, () -> {
							writePending.setFalse();
							reg.run();
						});
					} catch (IOException ex) {
						throw new RuntimeException(ex);
					}
				else
					fail();
				writePending.setTrue();
				reg.run();
				return Bytes.empty;
			};

			np.onConnected.fire(sender);

			sc.register(selector, SelectionKey.OP_READ, rr);
			sc.register(selector, SelectionKey.OP_WRITE, rw);
			reg.run();
		}
	}

	public void asyncConnect(InetSocketAddress address, IoSink<SocketChannel> sink) throws IOException {
		var sc = SocketChannel.open();
		sc.configureBlocking(false);
		sc.connect(address);
		reg(sc, SelectionKey.OP_CONNECT, sink);

	}

	public Closeable asyncListen(int port, IoSink<SocketChannel> sink) throws IOException {
		var ssc = ServerSocketChannel.open();
		ssc.configureBlocking(false);
		ssc.socket().bind(new InetSocketAddress(port));
		reg(ssc, SelectionKey.OP_ACCEPT, sink);
		return () -> Object_.closeQuietly(ssc);
	}

	public void asyncReadLine(SocketChannel sc, byte delim, IoSink<Bytes> sink) throws IOException {
		var bb = getReadBuffer(sc);

		new IoSink<Integer>() {
			public void sink(Integer start) throws IOException {
				var bytes_ = bb.toBytes();

				for (int i = start; i < bytes_.size(); i++)
					if (bytes_.get(i) == delim) {
						sink.sink(bytes_.range(0, i));
						bb.clear();
						bb.append(bytes_.range(i + 1));
						return;
					}

				asyncRead(sc, bytes1 -> {
					var size0 = bb.size();
					bb.append(bytes1);
					this.sink(size0);
				});
			}
		}.sink(0);
	}

	public void asyncRead(SocketChannel sc, int n, IoSink<Bytes> sink) throws IOException {
		var bb = getReadBuffer(sc);

		new IoSink<Void>() {
			public void sink(Void v) throws IOException {
				if (n <= bb.size()) {
					var bytes_ = bb.toBytes();
					sink.sink(bytes_.range(0, n));
					bb.clear();
					bb.append(Bytes.of(bytes_.range(n)));
				} else
					asyncRead(sc, bytes1 -> {
						bb.append(bytes1);
						this.sink(null);
					});
			}
		}.sink(null);
	}

	public void asyncRead(SocketChannel sc, IoSink<Bytes> sink) throws ClosedChannelException {
		reg(sc, SelectionKey.OP_READ, sink);
	}

	public void asyncWriteAll(SocketChannel sc, Bytes bytes, Runnable runnable) throws IOException {
		new IoSink<Bytes>() {
			public void sink(Bytes bytes) throws IOException {
				if (0 < bytes.size())
					asyncWrite(sc, bytes, this);
				else
					runnable.run();
			}
		}.sink(bytes);
	}

	public void asyncWrite(SocketChannel sc, Bytes bytes, IoSink<Bytes> sink) throws ClosedChannelException {
		IoSink<Object> runnable1 = dummy -> sink.sink(bytes.range(sc.write(bytes.toByteBuffer())));

		reg(sc, SelectionKey.OP_WRITE, runnable1);
	}

	public void close(SocketChannel sc) throws IOException {
		sc.register(selector, 0, null);
		sc.close();
	}

	public void run() throws IOException {
		while (isRunning) {

			// unfortunately Selector.wakeup() does not work on my Linux
			// machines. Thus we specify a time out to allow the selector
			// freed out temporarily; otherwise the register() methods in
			// other threads might block forever.
			selector.select(500);

			// this seems to allow other threads to gain access. Not exactly
			// the behavior as documented in NIO, but anyway.
			wakeUpSelector();

			var iter = selector.selectedKeys().iterator();

			while (iter.hasNext()) {
				var key = iter.next();
				iter.remove();

				try {
					processKey(key);
				} catch (Exception ex) {
					LogUtil.error(ex);
				}
			}
		}
	}

	private void processKey(SelectionKey key) throws IOException {
		// logUtil.info("KEY", dumpKey(key));

		var buffer = new byte[Defaults.bufferSize];
		@SuppressWarnings("unchecked")
		var callback = (IoSink<Object>) key.attachment();
		var ops = key.readyOps();
		var sc0 = key.channel();
		var sc1 = sc0 instanceof SocketChannel ? (SocketChannel) sc0 : null;

		reg(sc0, 0);

		if ((ops & SelectionKey.OP_ACCEPT) != 0) {
			var sc = ((ServerSocketChannel) sc0).accept().socket().getChannel();
			sc.configureBlocking(false);
			callback.sink(sc);
			reg(sc0, SelectionKey.OP_ACCEPT);
		}

		if ((ops & SelectionKey.OP_CONNECT) != 0) {
			sc1.finishConnect();
			callback.sink(sc1);
		}

		if ((ops & SelectionKey.OP_READ) != 0) {
			var n = sc1.read(ByteBuffer.wrap(buffer));
			if (0 <= n)
				callback.sink(Bytes.of(buffer, 0, n));
			else {
				onDisconnected.fire(sc1);
				sc1.close();
			}
		}

		if ((ops & SelectionKey.OP_WRITE) != 0)
			callback.sink(null);
	}

	private BytesBuilder getReadBuffer(SocketChannel sc) {
		return reads.computeIfAbsent(sc, sc_ -> new BytesBuilder());
	}

	private void reg(SelectableChannel sc, int key, Object attachment) throws ClosedChannelException {
		sc.register(selector, key, attachment);
		reg(sc, key);
	}

	private void reg(SelectableChannel sc, int key) {
		sc.keyFor(selector).interestOps(key);
		wakeUpSelector();
	}

	private void wakeUpSelector() {
		// selector.wakeup(); // not working in Windows machines
	}

}
