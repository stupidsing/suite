package suite.os;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;

import primal.Nouns.Buffer;
import primal.Verbs.RunnableEx;
import primal.fp.Funs.Source;
import primal.os.Log_;
import primal.primitive.adt.Bytes;

public class ListenNio {

	public interface IoAsync {
		public void read(Bytes in);

		public Bytes write();

		public void registerWrite(RunnableEx sink);
	}

	private Source<IoAsync> handleIo;
	private Selector selector;

	public ListenNio(Source<IoAsync> handleIo) {
		this.handleIo = handleIo;
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

			while (true)
				selector.select(key -> {
					try {
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
				});
		} catch (IOException ex) {
			throw new RuntimeException(ex);
		}
	}

	private void handleAccept(SocketChannel sc, SelectionKey key) throws IOException {
		sc.configureBlocking(false);
		var io = handleIo.g();
		io.registerWrite(() -> sc.register(selector, SelectionKey.OP_WRITE, io));
		sc.register(selector, SelectionKey.OP_READ, io);
	}

	private void handleRead(SocketChannel sc, IoAsync io) throws IOException {
		var bs = new byte[Buffer.size];
		var n = sc.read(ByteBuffer.wrap(bs));
		io.read(0 <= n ? Bytes.of(bs, 0, n) : null);
	}

	private void handleWrite(SocketChannel sc, IoAsync io) throws IOException {
		var bytes = io.write();

		if (bytes != null) {
			sc.write(ByteBuffer.wrap(bytes.bs, bytes.start, bytes.end));
			sc.register(selector, SelectionKey.OP_WRITE, io);
		} else
			sc.close();
	}

}
