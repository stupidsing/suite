package suite.os;

import static primal.statics.Rethrow.ex;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Comparator;
import java.util.List;

import primal.Nouns.Buffer;
import primal.adt.Fixie_.FixieFun3;
import primal.fp.Funs.Fun;
import primal.fp.Funs.Sink;
import primal.fp.Funs.Source;
import primal.os.Log_;
import primal.primitive.adt.Bytes;
import suite.adt.PriorityQueue;

public class ListenNio {

	private Fun<FixieFun3<Integer, Sink<Bytes>, Source<Bytes>, Object>, Object> ioFactory;
	private Selector selector;
	private PriorityQueue<Wait> waits = new PriorityQueue<>(Wait.class, 256, Comparator.comparingLong(w -> w.k));

	private class Attach {
		private SocketChannel sc;
		private Sink<Bytes> rd;
		private Source<Bytes> wr;

		private Attach(SocketChannel sc, Sink<Bytes> rd, Source<Bytes> wr) {
			this.sc = sc;
			this.rd = rd;
			this.wr = wr;
		}
	}

	public static class Wait {
		private long k;
		private Source<List<Wait>> v;

		public Wait(long k, Source<List<Wait>> v) {
			this.k = k;
			this.v = v;
		}
	}

	public ListenNio(Fun<FixieFun3<Integer, Sink<Bytes>, Source<Bytes>, Object>, Object> ioFactory) {
		this.ioFactory = ioFactory;
	}

	public void run(int port) {
		try {
			selector = Selector.open();

			// we have to set connection host, port and non-blocking mode
			var ssc = ServerSocketChannel.open();
			ssc.configureBlocking(false);
			ssc.register(selector, SelectionKey.OP_ACCEPT, null);

			var ss = ssc.socket();
			ss.bind(new InetSocketAddress("localhost", port));

			while (true) {
				var wait = waits.min();
				long nextWakeUp, timeout;

				if (wait != null) {
					nextWakeUp = wait.k;
					timeout = Math.max(1, nextWakeUp - System.currentTimeMillis());
				} else {
					nextWakeUp = Long.MAX_VALUE;
					timeout = 0l;
				}

				selector.select(key -> {
					try {
						if (nextWakeUp < System.currentTimeMillis())
							wait.v.g().forEach(waits::add);
						if (key.isAcceptable())
							handleAccept(ssc.accept(), key);
						if (key.isConnectable())
							;
						if (key.isReadable())
							handleRead((Attach) key.attachment());
						if (key.isWritable())
							handleWrite((Attach) key.attachment());
					} catch (Exception ex) {
						Log_.error(ex);
					}
				}, timeout);
			}
		} catch (IOException ex) {
			throw new RuntimeException(ex);
		}
	}

	public void wait(Wait wait) {
		waits.add(wait);
	}

	private void handleAccept(SocketChannel sc, SelectionKey key) throws IOException {
		sc.configureBlocking(false);

		ioFactory.apply((k, rd, wr) -> {
			var attach = new Attach(sc, rd, wr);
			return ex(() -> sc.register(selector, k, attach));
		});
	}

	private void handleRead(Attach attach) throws IOException {
		var bs = new byte[Buffer.size];
		var n = attach.sc.read(ByteBuffer.wrap(bs));
		var rd = attach.rd;

		if (rd != null)
			rd.f(0 <= n ? Bytes.of(bs, 0, n) : null);
	}

	private void handleWrite(Attach attach) throws IOException {
		var wr = attach.wr;

		if (wr != null) {
			var bytes = wr.g();

			if (bytes != null)
				attach.sc.write(ByteBuffer.wrap(bytes.bs, bytes.start, bytes.end));
			else
				attach.sc.close();
		}
	}

}
