package suite.os;

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
import primal.fp.Funs.Source;
import primal.os.Log_;
import primal.primitive.adt.Bytes;
import suite.adt.PriorityQueue;

public class ListenNio {

	public interface IoAsync {
		public int getSelectionKey();

		public void read(Bytes in);

		public Bytes write();
	}

	private Source<IoAsync> ioAsyncFactory;
	private Selector selector;
	private PriorityQueue<Wait> waits = new PriorityQueue<>(Wait.class, 256, Comparator.comparingLong(w -> w.k));

	public static class Wait {
		private long k;
		private Source<List<Wait>> v;

		public Wait(long k, Source<List<Wait>> v) {
			this.k = k;
			this.v = v;
		}
	}

	public ListenNio(Source<IoAsync> ioAsyncFactory) {
		this.ioAsyncFactory = ioAsyncFactory;
	}

	public void run(int port) {
		try {
			selector = Selector.open();

			// we have to set connection host, port and non-blocking mode
			var ssc = ServerSocketChannel.open();
			ssc.configureBlocking(false);
			ssc.register(selector, ssc.validOps(), null);

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
							wait.v.g().forEach(waits::insert);
						if (key.isAcceptable())
							handleAccept(ssc.accept(), key);
						if (key.isConnectable())
							;
						if (key.isReadable())
							handleRead((SocketChannel) key.channel(), (IoAsync) key.attachment());
						if (key.isWritable())
							handleWrite((SocketChannel) key.channel(), (IoAsync) key.attachment());
					} catch (Exception ex) {
						Log_.error(ex);
					}
				}, timeout);
			}
		} catch (IOException ex) {
			throw new RuntimeException(ex);
		}
	}

	private void handleAccept(SocketChannel sc, SelectionKey key) throws IOException {
		sc.configureBlocking(false);
		var io = ioAsyncFactory.g();
		sc.register(selector, io.getSelectionKey(), io);
	}

	private void handleRead(SocketChannel sc, IoAsync io) throws IOException {
		var bs = new byte[Buffer.size];
		var n = sc.read(ByteBuffer.wrap(bs));
		io.read(0 <= n ? Bytes.of(bs, 0, n) : null);
		sc.register(selector, io.getSelectionKey(), io);
	}

	private void handleWrite(SocketChannel sc, IoAsync io) throws IOException {
		var bytes = io.write();

		if (bytes != null) {
			sc.write(ByteBuffer.wrap(bytes.bs, bytes.start, bytes.end));
			sc.register(selector, io.getSelectionKey(), io);
		} else
			sc.close();
	}

}
