package suite.net;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.concurrent.ThreadPoolExecutor;

import suite.net.Bytes.BytesBuilder;
import suite.util.FunUtil.Fun;
import suite.util.FunUtil.FunEx;
import suite.util.LogUtil;
import suite.util.Util;

public class Channels {

	/**
	 * Channel that will reconnect if failed for any reasons.
	 */
	public abstract static class PersistableChannel<CL extends Channel> extends RequestResponseChannel {
		private NioDispatcher<CL> dispatcher;
		private InetSocketAddress address;
		boolean isStarted;

		public PersistableChannel(NioDispatcher<CL> dispatcher //
				, RequestResponseMatcher matcher //
				, ThreadPoolExecutor executor //
				, InetSocketAddress address //
				, Fun<Bytes, Bytes> handler) {
			super(matcher, executor, handler);
			this.dispatcher = dispatcher;
			this.address = address;
		}

		public synchronized void start() {
			isStarted = true;
			reconnect();
		}

		public synchronized void stop() {
			isStarted = false;
		}

		@Override
		public void onClose() {
			reconnect();
		}

		private void reconnect() {
			if (isStarted && !isConnected())
				try {
					dispatcher.reconnect(this, address);
				} catch (IOException ex) {
					throw new RuntimeException(ex);
				}
		}
	}

	/**
	 * Channel that exhibits client/server message exchange.
	 */
	public static class RequestResponseChannel extends PacketChannel {
		private static final char RESPONSE = 'P';
		static final char REQUEST = 'Q';

		private RequestResponseMatcher matcher;
		private ThreadPoolExecutor executor;
		private boolean isConnected;
		private Fun<Bytes, Bytes> handler;

		public RequestResponseChannel(RequestResponseMatcher matcher, ThreadPoolExecutor executor, Fun<Bytes, Bytes> handler) {
			this.matcher = matcher;
			this.executor = executor;
			this.handler = handler;
		}

		public void send(char type, int token, Bytes data) {
			if (!isConnected)
				synchronized (this) {
					while (!isConnected)
						Util.wait(this);
				}

			sendPacket(new BytesBuilder() //
					.append((byte) type) //
					.append(NetUtil.bytesValue(token)) //
					.append(data) //
					.toBytes());
		}

		public boolean isConnected() {
			return isConnected;
		}

		@Override
		public void onConnected(Sender sender) {
			setConnected(true);
			super.onConnected(sender);
		}

		@Override
		public void onClose() {
			setConnected(false);
		}

		@Override
		public void onReceivePacket(Bytes packet) {
			if (packet.size() >= 5) {
				char type = (char) packet.byteAt(0);
				final int token = NetUtil.intValue(packet.subbytes(1, 5));
				final Bytes contents = packet.subbytes(5);

				if (type == RESPONSE)
					matcher.onRespondReceived(token, contents);
				else if (type == REQUEST)
					executor.execute(new Runnable() {
						public void run() {
							send(RESPONSE, token, handler.apply(contents));
						}
					});
			}
		}

		private synchronized void setConnected(boolean isConnected) {
			this.isConnected = isConnected;
			notify();
		}
	}

	/**
	 * Channel that transfer data in the unit of packets.
	 */
	public abstract static class PacketChannel extends BufferedChannel {
		private Bytes received = Bytes.emptyBytes;

		public abstract void onReceivePacket(Bytes packet);

		protected void sendPacket(Bytes packet) {
			send(new BytesBuilder() //
					.append(NetUtil.bytesValue(packet.size())) //
					.append(packet) //
					.toBytes());
		}

		@Override
		public final void onReceive(Bytes message) {
			received = received.append(message);

			if (received.size() >= 4) {
				int end = 4 + NetUtil.intValue(received.subbytes(0, 4));

				if (received.size() >= end) {
					Bytes packet = received.subbytes(4, end);
					received = received.subbytes(end);
					onReceivePacket(packet);
				}
			}
		}
	}

	/**
	 * Channel with a send buffer.
	 */
	public abstract static class BufferedChannel implements Channel {
		private Sender sender;
		private Bytes toSend = Bytes.emptyBytes;

		public void send(Bytes message) {
			toSend = toSend.append(message);

			try {
				onTrySend();
			} catch (IOException ex) {
				LogUtil.error(ex);
			}
		}

		@Override
		public void onConnected(Sender sender) {
			this.sender = sender;
		}

		@Override
		public void onClose() {
		}

		@Override
		public void onTrySend() throws IOException {
			toSend = sender.apply(toSend);
		}

	}

	public interface Channel {
		public void onConnected(Sender sender);

		public void onClose();

		public void onReceive(Bytes message);

		public void onTrySend() throws IOException;
	}

	public interface Sender extends FunEx<Bytes, Bytes, IOException> {
	}

}
