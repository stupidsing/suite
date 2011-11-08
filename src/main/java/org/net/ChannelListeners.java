package org.net;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.atomic.AtomicInteger;

import org.net.Bytes.BytesBuilder;
import org.net.NioDispatcher.ChannelListener;
import org.util.LogUtil;
import org.util.Util;
import org.util.Util.IoProcess;

public abstract class ChannelListeners implements ChannelListener {

	public abstract static class PersistableChannel<CL extends ChannelListener>
			extends RequestResponseChannel {
		private NioDispatcher<CL> dispatcher;
		private InetSocketAddress address;
		boolean started;

		public PersistableChannel(NioDispatcher<CL> dispatcher //
				, RequestResponseMatcher matcher //
				, ThreadPoolExecutor executor //
				, InetSocketAddress address) {
			super(matcher, executor);
			this.dispatcher = dispatcher;
			this.address = address;
		}

		public synchronized void start() {
			started = true;
			reconnect();
		}

		public synchronized void stop() {
			started = false;
		}

		@Override
		public void onClose() {
			super.onClose();
			reconnect();
		}

		private void reconnect() {
			if (started && !isConnected())
				try {
					dispatcher.reconnect(this, address);
				} catch (IOException ex) {
					throw new RuntimeException(ex);
				}
		}
	}

	public abstract static class RequestResponseChannel extends PacketChannel {
		private static final char RESPONSE = 'P';
		private static final char REQUEST = 'Q';

		private RequestResponseMatcher matcher;
		private ThreadPoolExecutor executor;
		private boolean connected;

		public RequestResponseChannel(RequestResponseMatcher matcher,
				ThreadPoolExecutor executor) {
			this.matcher = matcher;
			this.executor = executor;
		}

		public abstract Bytes respondToRequest(Bytes request);

		@Override
		public void onConnected() {
			setConnected(true);
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
					matcher.onRespond(token, contents);
				else if (type == REQUEST)
					executor.execute(new Runnable() {
						public void run() {
							send(RESPONSE, token, respondToRequest(contents));
						}
					});
			}
		}

		private void send(char type, int token, Bytes data) {
			if (!connected)
				synchronized (this) {
					while (!connected)
						Util.wait(this);
				}

			sendPacket(new BytesBuilder() //
					.append((byte) type) //
					.append(NetUtil.bytesValue(token)) //
					.append(data) //
					.toBytes());
		}

		public boolean isConnected() {
			return connected;
		}

		private synchronized void setConnected(boolean isConnected) {
			connected = isConnected;
			notify();
		}
	}

	public static class RequestResponseMatcher {
		private static final AtomicInteger tokenCounter = new AtomicInteger();

		// TODO clean-up lost requests
		private Map<Integer, Bytes[]> requests = new HashMap<Integer, Bytes[]>();

		private void onRespond(int token, Bytes respond) {
			Bytes holder[] = requests.get(token);

			if (holder != null)
				synchronized (holder) {
					holder[0] = respond;
					holder.notify();
				}
		}

		public Bytes requestForResponse(RequestResponseChannel channel,
				Bytes request) {
			return requestForResponse(channel, request, 0);
		}

		public Bytes requestForResponse(RequestResponseChannel channel,
				Bytes request, int timeOut) {
			Integer token = tokenCounter.getAndIncrement();
			Bytes holder[] = new Bytes[1];

			synchronized (holder) {
				requests.put(token, holder);
				channel.send(RequestResponseChannel.REQUEST, token, request);

				while (holder[0] == null)
					Util.wait(holder, timeOut);

				requests.remove(token);
			}

			return holder[0];
		}
	}

	public abstract static class PacketChannel extends BufferedChannel {
		private Bytes received = Bytes.EMPTYBYTES;

		public abstract void onReceivePacket(Bytes packet);

		@Override
		public final void onReceive(Bytes message) {
			received = received.append(message);
			Bytes packet = receivePacket();

			if (packet != null)
				onReceivePacket(packet);
		}

		protected void sendPacket(Bytes packet) {
			send(new BytesBuilder() //
					.append(NetUtil.bytesValue(packet.size())) //
					.append(packet) //
					.toBytes());
		}

		protected Bytes receivePacket() {
			Bytes packet = null;

			if (received.size() >= 4) {
				int end = 4 + NetUtil.intValue(received.subbytes(0, 4));

				if (received.size() >= end) {
					packet = received.subbytes(4, end);
					received = received.subbytes(end);
				}
			}

			return packet;
		}
	}

	public abstract static class BufferedChannel implements ChannelListener {
		private IoProcess<Bytes, Bytes, IOException> sender;
		private Bytes toSend = Bytes.EMPTYBYTES;

		@Override
		public void onConnected() {
		}

		@Override
		public void onClose() {
		}

		@Override
		public void trySend() throws IOException {
			toSend = sender.perform(toSend);
		}

		@Override
		public void setTrySendDelegate(
				IoProcess<Bytes, Bytes, IOException> sender) {
			this.sender = sender;
		}

		public void send(Bytes message) {
			toSend = toSend.append(message);

			try {
				trySend();
			} catch (IOException ex) {
				LogUtil.error(getClass(), ex);
			}
		}
	}

}
