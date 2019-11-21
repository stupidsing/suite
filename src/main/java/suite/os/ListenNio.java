package suite.os;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;

import primal.fp.Funs.Source;
import primal.os.Log_;
import primal.primitive.adt.Bytes;
import primal.puller.Puller;

public class ListenNio {

	public interface IoAsync {
		public Puller<Bytes> read(Bytes in);
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

			while (true) {
				selector.select();
				var iter = selector.selectedKeys().iterator();

				while (iter.hasNext()) {
					var key = iter.next();
					iter.remove();

					try {
						if (key.isAcceptable())
							handleAccept(ssc.accept(), key);
						if (key.isConnectable())
							;
						if (key.isReadable())
							handleRead((SocketChannel) key.channel(), (IoAsync) key.attachment());
						if (key.isWritable())
							handleWrite((SocketChannel) key.channel(), (Puller<?>) key.attachment());
					} catch (Exception ex) {
						Log_.error(ex);
					}
				}
			}
		} catch (IOException ex) {
			throw new RuntimeException(ex);
		}
	}

	private void handleAccept(SocketChannel sc, SelectionKey key) throws IOException {
		sc.configureBlocking(false);
		sc.register(selector, SelectionKey.OP_READ, handleIo.g());
	}

	private void handleRead(SocketChannel sc, IoAsync io) throws IOException {
		var bs = new byte[1024];
		var n = sc.read(ByteBuffer.wrap(bs));
		var puller = io.read(0 < n ? Bytes.of(bs, 0, n) : null);

		if (puller != null)
			sc.register(selector, SelectionKey.OP_WRITE, puller);
	}

	private void handleWrite(SocketChannel sc, Puller<?> puller) throws IOException {
		var bs = (Bytes) puller.pull();

		if (bs != null) {
			sc.write(ByteBuffer.wrap(bs.bs, bs.start, bs.end));
			sc.register(selector, SelectionKey.OP_WRITE, puller);
		} else
			sc.close();
	}

}
