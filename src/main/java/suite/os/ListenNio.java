package suite.os;

import primal.Nouns.Buffer;
import primal.fp.Funs.Sink;
import primal.fp.Funs.Source;
import primal.os.Log_;
import primal.primitive.IntPrim.IntSink;
import primal.primitive.adt.Bytes;
import suite.adt.PriorityQueue;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Comparator;

import static java.lang.Math.max;
import static primal.statics.Rethrow.ex;

public class ListenNio {

	private Sink<Reg> accept;
	private Selector selector;
	private PriorityQueue<Sleep> sleeps = new PriorityQueue<>(Sleep.class, 256, Comparator.comparingLong(w -> w.k));

	public interface Reg {
		public Object listen(int key, Sink<Bytes> rd, Source<Bytes> wr, IntSink wrt);

		public void sleep(long ms, Runnable runnable);

		public default void listenRead(Sink<Bytes> rd) {
			listen(SelectionKey.OP_READ, rd, null, null);
		}

		public default void listenWrite(Source<Bytes> wr, IntSink wrt) {
			listen(SelectionKey.OP_WRITE, null, wr, wrt);
		}
	}

	public static class Sleep {
		private long k;
		private Runnable v;

		public Sleep(long k, Runnable v) {
			this.k = k;
			this.v = v;
		}
	}

	private class Attach {
		private SocketChannel sc;
		private Sink<Bytes> rd;
		private Source<Bytes> wr;
		private IntSink wrt;

		private Attach(SocketChannel sc, Sink<Bytes> rd, Source<Bytes> wr, IntSink wrt) {
			this.sc = sc;
			this.rd = rd;
			this.wr = wr;
			this.wrt = wrt;
		}
	}

	public ListenNio(Sink<Reg> accept) {
		this.accept = accept;
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
				var sleep = sleeps.min();
				long wakeUp, timeout;

				if (sleep != null) {
					wakeUp = sleep.k;
					timeout = max(1, wakeUp - System.currentTimeMillis());
				} else {
					wakeUp = Long.MAX_VALUE;
					timeout = 0l;
				}

				selector.select(key -> {
					try {
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

				if (wakeUp < System.currentTimeMillis())
					sleeps.extractMin().v.run();
			}
		} catch (IOException ex) {
			throw new RuntimeException(ex);
		}
	}

	public void sleep(long ms, Runnable runnable) {
		sleep_(ms, runnable);
	}

	private void handleAccept(SocketChannel sc, SelectionKey key) throws IOException {
		sc.configureBlocking(false);

		accept.f(new Reg() {
			public Object listen(int key, Sink<Bytes> rd, Source<Bytes> wr, IntSink wrt) {
				var attach = new Attach(sc, rd, wr, wrt);
				return ex(() -> sc.register(selector, key, attach));
			}

			public void sleep(long ms, Runnable runnable) {
				ListenNio.this.sleep_(ms, runnable);
			}
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
			if (bytes != null) {
				var bb = ByteBuffer.wrap(bytes.bs, bytes.start, bytes.size());
				int n;
				try {
					n = attach.sc.write(bb);
				} catch (IOException ex) {
					n = -1;
				}
				attach.wrt.f(n);
			} else
				attach.sc.close();
		}
	}

	private void sleep_(long ms, Runnable runnable) {
		sleeps.add(new Sleep(System.currentTimeMillis() + ms, runnable));
	}

}
