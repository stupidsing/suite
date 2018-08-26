package suite.net.nio;

import static suite.util.Friends.rethrow;

import java.net.InetSocketAddress;
import java.util.concurrent.ExecutorService;

import suite.concurrent.Condition;
import suite.net.NetUtil;
import suite.primitive.Bytes;
import suite.primitive.Bytes.BytesBuilder;
import suite.streamlet.FunUtil.Iterate;
import suite.streamlet.FunUtil.Sink;
import suite.streamlet.Signal;

public interface NioplexFactory {

	public static char RESPONSE = 'P';
	public static char REQUEST = 'Q';

	public class PersistentNioplex extends RequestResponseNioplex {
		private NioDispatcher<PersistentNioplex> nio;
		private InetSocketAddress address;
		private boolean isStarted;

		public PersistentNioplex(NioDispatcher<PersistentNioplex> nio, InetSocketAddress address) {
			this.nio = nio;
			this.address = address;
		}

		public void start() {
			isStarted = true;
			reconnect();
		}

		public void stop() {
			isStarted = false;
		}

		private void reconnect() {
			if (isStarted && !isConnected())
				rethrow(() -> {
					nio.reconnect(this, address);
					return nio;
				});
		}
	}

	public class RequestResponseNioplex extends PacketedNioplex {
		public static char RESPONSE = 'P';
		public static char REQUEST = 'Q';
		private boolean isConnected;
		private Condition condition = new Condition(() -> isConnected);

		public void send(char type, int token, Bytes data) {
			var packet = new BytesBuilder() //
					.append((byte) type) //
					.append(NetUtil.intToBytes(token)) //
					.append(data) //
					.toBytes();

			if (!isConnected)
				condition.waitThen(() -> {
				}, () -> true);

			sendPacket(packet);
		}

		public void setConnected(boolean isConnected) {
			condition.thenNotify(() -> this.isConnected = isConnected);
		}

		public boolean isConnected() {
			return isConnected;
		}
	}

	public class PacketedNioplex extends BufferedNioplex {
		public final Signal<Bytes> onReceivePacket = Signal.of();

		public void sendPacket(Bytes packet) {
			send(new BytesBuilder() //
					.append(NetUtil.intToBytes(packet.size())) //
					.append(packet) //
					.toBytes());
		}
	}

	public class BufferedNioplex extends Nioplex {
		private Bytes toSend = Bytes.empty;
		private Iterate<Bytes> sender;

		public void send(Bytes out) {
			toSend = toSend.append(out);
			trySend();
		}

		public void setSender(Iterate<Bytes> sender) {
			this.sender = sender;
			trySend();
		}

		public void trySend() {
			if (sender != null)
				toSend = sender.apply(toSend);
		}
	}

	public class Nioplex {
		public final Signal<Iterate<Bytes>> onConnected = Signal.of();
		public final Signal<Bytes> onReceive = Signal.of();
		public final Signal<Boolean> onTrySend = Signal.of();
	}

	public static <NP extends PersistentNioplex> NP persistent( //
			NP np0, //
			RequestResponseMatcher matcher, //
			ExecutorService executor, //
			Iterate<Bytes> handler) {
		return requestResponse(np0, matcher, executor, handler);
	}

	public static <NP extends RequestResponseNioplex> NP requestResponse( //
			NP np0, //
			RequestResponseMatcher matcher, //
			ExecutorService executor, //
			Iterate<Bytes> handler) {
		var np = packeted(np0);
		np.onConnected.wire(sender -> np.setConnected(sender != null));
		np.onReceivePacket.wire(packet -> {
			if (5 <= packet.size()) {
				var type = (char) packet.get(0);
				var token = NetUtil.bytesToInt(packet.range(1, 5));
				var contents = packet.range(5);

				if (type == RESPONSE)
					matcher.onResponseReceived(token, contents);
				else if (type == REQUEST)
					executor.execute(() -> np.send(RESPONSE, token, handler.apply(contents)));
			}
		});
		return np;
	}

	public static <NP extends PacketedNioplex> NP packeted(NP np0) {
		var np = buffered(np0);
		np.onReceive.wire(new Sink<>() {
			private Bytes received = Bytes.empty;

			public void sink(Bytes message) {
				received = received.append(message);
				var size = received.size();

				if (4 <= size) {
					var end = 4 + NetUtil.bytesToInt(received.range(0, 4));

					if (end <= size) {
						var in = received.range(4, end);
						received = received.range(end);
						np.onReceivePacket.fire(in);
					}
				}
			}
		});
		return np;
	}

	public static <NP extends BufferedNioplex> NP buffered(NP np) {
		np.onConnected.wire(np::setSender);
		np.onTrySend.wire(np::trySend);
		return np;
	}

}
