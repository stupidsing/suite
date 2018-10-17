package suite.net.nio;

import static suite.util.Friends.forInt;
import static suite.util.Friends.rethrow;

import java.io.Closeable;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.NotYetConnectedException;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.HashMap;
import java.util.Map;

import suite.adt.PriorityQueue;
import suite.cfg.Defaults;
import suite.concurrent.Backoff;
import suite.concurrent.Pool;
import suite.net.NetUtil;
import suite.object.Object_;
import suite.os.LogUtil;
import suite.primitive.Bytes;
import suite.primitive.Bytes.BytesBuilder;
import suite.streamlet.FunUtil.Iterate;
import suite.streamlet.FunUtil.Sink;
import suite.streamlet.FunUtil2.Sink2;
import suite.util.Util;

public class NioDispatch implements Closeable {

	private boolean isRunning = true;
	private Selector selector = Selector.open();
	private ThreadLocal<byte[]> threadBuffer = ThreadLocal.withInitial(() -> new byte[Defaults.bufferSize]);

	private PriorityQueue<TimeDispatch> timeDispatches = new PriorityQueue<>( //
			TimeDispatch.class, //
			256, //
			(td0, td1) -> Long.compare(td0.time, td1.time));

	private class TimeDispatch {
		private long time;
		private Runnable runnable;

		private TimeDispatch(long t0, Runnable t1) {
			this.time = t0;
			this.runnable = t1;
		}
	}

	public NioDispatch() throws IOException {
		timeDispatches.insert(new TimeDispatch(Long.MAX_VALUE, null));
	}

	@Override
	public void close() throws IOException {
		stop();
		selector.close();
	}

	public void stop() {
		isRunning = false;
	}

	public class Requester {
		private Map<Integer, Sink<Bytes>> handlers = new HashMap<>();
		private Reconnect reconnect;
		private PacketId packetId;
		private Runnable reader;

		public Requester(InetSocketAddress address) {
			reconnect = new Reconnect(address, rec -> {
				packetId = new PacketId(rec.rw);

				reader = new Runnable() {
					public void run() {
						packetId.read((id_, bs) -> {
							handlers.remove(id_).f(bs);
							run();
						}, rec.reconnect);
					}
				};
			});
		}

		public void request(Bytes request, Sink<Bytes> okay) {
			var id = Util.temp();
			handlers.put(id, okay);
			reconnect.connect(rec -> packetId.write(id, request, v -> reader.run(), rec.reconnect));
		}
	}

	public class Responder {
		public Closeable listen(int port, Iterate<Bytes> fun, Sink<IOException> fail) {
			Sink<IOException> failRequest = LogUtil::error;

			return asyncListen(port, rw -> {
				PacketId packetId = new PacketId(rw);
				new Object() {
					public void run() {
						packetId.read((id, bs) -> packetId.write(id, fun.apply(bs), v -> run(), failRequest), failRequest);
					}
				}.run();
			}, fail);
		}
	}

	public class ReconnectPool {
		private Pool<Reconnect> pool;

		public ReconnectPool(InetSocketAddress address, Sink<Reconnectable> connected) {
			pool = Pool.of(forInt(9).map(i -> new Reconnect(address, connected)).toArray(Reconnect.class));
		}

		public Closeable connect(Sink<Reconnectable> okay) {
			var reconnect = pool.get();
			reconnect.connect(okay);
			return () -> pool.unget(reconnect);
		}
	}

	public class Reconnectable {
		public final AsyncRw rw;
		public final Sink<IOException> reconnect;

		public Reconnectable(AsyncRw rw, Sink<IOException> reconnect) {
			this.rw = rw;
			this.reconnect = reconnect;
		}
	}

	public class Reconnect {
		private InetSocketAddress address;
		private Sink<Reconnectable> connected;
		private Reconnectable rec;
		private Backoff backoff = new Backoff();

		public Reconnect(InetSocketAddress address, Sink<Reconnectable> connected) {
			this.address = address;
			this.connected = connected;
		}

		public void connect(Sink<Reconnectable> okay) {
			if (rec == null)
				asyncConnect(address, rw_ -> {
					var r = new Reconnectable(rw_, this::reset);
					connected.f(rec = r);
					okay.f(r);
				}, ex -> {
					reset(ex);
					timeDispatches.insert(new TimeDispatch(System.currentTimeMillis() + backoff.duration(), () -> connect(okay)));
				});
			else
				okay.f(rec);
		}

		public void reset(Exception ex) {
			LogUtil.error(ex);
			rec.rw.close();
			rec = null;
		}
	}

	public class PacketId {
		private Buffer buffer;
		private Packet packet;

		public PacketId(AsyncRw rw) {
			buffer = new Buffer(rw);
			packet = new Packet(buffer);
		}

		public void read(Sink2<Integer, Bytes> okay, Sink<IOException> fail) {
			buffer.read(4, bs0 -> packet.read(bs1 -> okay.sink2(NetUtil.bytesToInt(bs0), bs1), fail), fail);
		}

		public void write(int id, Bytes bs, Sink<Void> okay, Sink<IOException> fail) {
			buffer.writeAll(NetUtil.intToBytes(id), v -> packet.write(bs, okay, fail), fail);
		}
	}

	public class Packet {
		private Buffer buffer;

		public Packet(Buffer buffer) {
			this.buffer = buffer;
		}

		public void read(Sink<Bytes> okay, Sink<IOException> fail) {
			buffer.read(4, bs0 -> buffer.read(NetUtil.bytesToInt(bs0), okay, fail), fail);
		}

		public void write(Bytes bs, Sink<Void> okay, Sink<IOException> fail) {
			buffer.writeAll(NetUtil.intToBytes(bs.size()), v -> buffer.writeAll(bs, okay, fail), fail);
		}
	}

	public class Buffer {
		private AsyncRw rw;
		private BytesBuilder bb = new BytesBuilder();

		public Buffer(AsyncRw rw) {
			this.rw = rw;
		}

		public void writeAll(Bytes bytes, Sink<Void> okay, Sink<IOException> fail) {
			new Object() {
				public void sink(int start) {
					if (start < bytes.size())
						rw.write(bytes.range(start), written -> sink(start + written), fail);
					else
						okay.f(null);
				}
			}.sink(0);
		}

		public void readLine(byte delim, Sink<Bytes> okay, Sink<IOException> fail) {
			new Object() {
				public void read_(int start) {
					var bytes_ = bb.toBytes();

					for (int i = start; i < bytes_.size(); i++)
						if (bytes_.get(i) == delim) {
							bb.clear();
							bb.append(bytes_.range(i + 1));
							okay.f(bytes_.range(0, i));
							return;
						}

					rw.read(bytes1 -> {
						var size0 = bb.size();
						bb.append(bytes1);
						read_(size0);
					}, fail);
				}
			}.read_(0);
		}

		public void read(int n, Sink<Bytes> okay, Sink<IOException> fail) {
			new Object() {
				public void read_() {
					if (n <= bb.size()) {
						var bytes_ = bb.toBytes();
						bb.clear();
						bb.append(bytes_.range(n));
						okay.f(bytes_.range(0, n));
					} else
						rw.read(bytes1 -> {
							bb.append(bytes1);
							read_();
						}, fail);
				}
			}.read_();
		}
	}

	public class AsyncRw {
		private SocketChannel sc;

		public AsyncRw(SocketChannel sc) {
			this.sc = sc;
		}

		public void close() {
			try {
				sc.register(selector, 0, null);
				sc.close();
			} catch (IOException ex) {
				LogUtil.error(ex);
			}
		}

		public void read(Sink<Bytes> sink0, Sink<IOException> fail) {
			Sink<Object> okay1 = object -> {
				if (object instanceof Bytes)
					sink0.f((Bytes) object);
				else if (object instanceof IOException)
					fail.f((IOException) object);
				else
					fail.f(null);
			};

			reg(sc, SelectionKey.OP_READ, okay1, fail);
		}

		public void write(Bytes bytes, Sink<Integer> okay0, Sink<IOException> fail) {
			Sink<Object> okay1 = dummy -> {
				try {
					okay0.f(sc.write(bytes.toByteBuffer()));
				} catch (IOException ex) {
					fail.f(ex);
				}
			};

			reg(sc, SelectionKey.OP_WRITE, okay1, fail);
		}
	}

	public void asyncConnect(InetSocketAddress address, Sink<AsyncRw> okay0, Sink<IOException> fail) {
		Sink<Object> okay1 = rw -> {
			if (rw instanceof AsyncRw)
				okay0.f((AsyncRw) rw);
			else
				fail.f(null);
		};

		try {
			var sc = SocketChannel.open();
			sc.configureBlocking(false);
			sc.connect(address);
			reg(sc, SelectionKey.OP_CONNECT, okay1, fail);
		} catch (IOException ex) {
			fail.f(ex);
		}
	}

	public Closeable asyncListen(int port, Sink<AsyncRw> accept) {
		return asyncListen(port, accept, LogUtil::error);
	}

	public Closeable asyncListen(int port, Sink<AsyncRw> accept, Sink<IOException> fail) {
		try {
			var ssc = ServerSocketChannel.open();
			ssc.configureBlocking(false);
			ssc.socket().bind(new InetSocketAddress(port));
			reg(ssc, SelectionKey.OP_ACCEPT, accept, fail);
			return () -> Object_.closeQuietly(ssc);
		} catch (IOException ex) {
			fail.f(ex);
			return null;
		}
	}

	public void run() {
		var now = System.currentTimeMillis();

		while (isRunning) {
			var td0 = timeDispatches.min();
			var tdw = td0.time;
			var wait = Math.max(0l, Math.min(500l, tdw - now));

			// unfortunately Selector.wakeup() does not work on my Linux
			// machines. Thus we specify a time out to allow the selector
			// freed out temporarily; otherwise the register() methods in
			// other threads might block forever.
			rethrow(() -> selector.select(wait));
			now = System.currentTimeMillis();

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

			if (tdw <= now)
				timeDispatches.extractMin().runnable.run();
		}
	}

	private void processKey(SelectionKey key) throws IOException {
		// logUtil.info("KEY", dumpKey(key));

		var buffer = threadBuffer.get();
		@SuppressWarnings("unchecked")
		var callback = (Sink<Object>) key.attachment();
		var ops = key.readyOps();
		var sc0 = key.channel();
		var sc1 = sc0 instanceof SocketChannel ? (SocketChannel) sc0 : null;

		reg(sc0, 0);

		if ((ops & SelectionKey.OP_ACCEPT) != 0) {
			var sc = ((ServerSocketChannel) sc0).accept().socket().getChannel();
			sc.configureBlocking(false);
			callback.f(new AsyncRw(sc));
			reg(sc0, SelectionKey.OP_ACCEPT);
		}

		if ((ops & SelectionKey.OP_CONNECT) != 0)
			callback.f(sc1.finishConnect() ? new AsyncRw(sc1) : null);

		if ((ops & SelectionKey.OP_READ) != 0) {
			try {
				var n = sc1.read(ByteBuffer.wrap(buffer));
				if (0 <= n)
					callback.f(Bytes.of(buffer, 0, n));
				else {
					callback.f(null);
					sc1.close();
				}
			} catch (ClosedChannelException | NotYetConnectedException ex) {
				callback.f(ex);
				sc1.close();
			}
		}

		if ((ops & SelectionKey.OP_WRITE) != 0)
			callback.f(null);
	}

	private void reg(SelectableChannel sc, int key, Sink<?> attachment, Sink<IOException> fail) {
		try {
			reg(sc, key, attachment);
		} catch (ClosedChannelException ex) {
			fail.f(ex);
		}
	}

	private void reg(SelectableChannel sc, int key, Sink<?> attachment) throws ClosedChannelException {
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
