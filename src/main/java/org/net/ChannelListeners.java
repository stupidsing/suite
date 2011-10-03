package org.net;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.atomic.AtomicInteger;

import org.net.NioDispatcher.ChannelListener;
import org.util.IoUtil;
import org.util.LogUtil;
import org.util.Util;
import org.util.Util.IoProcess;

public abstract class ChannelListeners implements ChannelListener {

	public abstract static class PersistableChannel<T extends PersistableChannel<T>>
			extends RequestResponseChannel {
		private NioDispatcher<T> dispatcher;
		private InetSocketAddress address;
		boolean started;

		public PersistableChannel(NioDispatcher<T> dispatcher,
				RequestResponseMatcher matcher, ThreadPoolExecutor executor,
				InetSocketAddress address) throws IOException {
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
			if (started)
				try {
					dispatcher.reconnect(this, address);
				} catch (IOException ex) {
					throw new RuntimeException(ex);
				}
		}
	}

	public abstract static class RequestResponseChannel extends PacketChannel {
		private final static char RESPONSE = 'P';
		private final static char REQUEST = 'Q';

		private RequestResponseMatcher matcher;
		private ThreadPoolExecutor executor;
		private boolean connected;

		public RequestResponseChannel(RequestResponseMatcher matcher,
				ThreadPoolExecutor executor) {
			this.matcher = matcher;
			this.executor = executor;
		}

		public abstract String respondForRequest(String request);

		@Override
		public void onConnected() {
			setConnected(true);
		}

		@Override
		public void onClose() {
			setConnected(false);
		}

		@Override
		public void onReceivePacket(String packet) {
			if (packet.length() >= 5) {
				char type = packet.charAt(0);
				final int token = intValue(packet.substring(1, 5));
				final String contents = packet.substring(5);

				if (type == RESPONSE)
					matcher.onRespond(token, contents);
				else if (type == REQUEST)
					executor.execute(new Runnable() {
						public void run() {
							send(RESPONSE, token, respondForRequest(contents));
						}
					});
			}
		}

		private void send(char type, int token, String data) {
			if (!connected)
				synchronized (this) {
					while (!connected)
						Util.wait(this);
				}

			sendPacket(type + strValue(token) + data);
		}

		private synchronized void setConnected(boolean isConnected) {
			connected = isConnected;
			notify();
		}
	}

	public static class RequestResponseMatcher {
		private final static AtomicInteger tokenCounter = new AtomicInteger();

		// TODO clean-up lost requests
		private Map<Integer, String[]> requests = new HashMap<Integer, String[]>();

		private void onRespond(int token, String respond) {
			String holder[] = requests.get(token);

			if (holder != null)
				synchronized (holder) {
					holder[0] = respond;
					holder.notify();
				}
		}

		public String requestForResponse(RequestResponseChannel listener,
				String request) {
			return requestForResponse(listener, request, 0);
		}

		public String requestForResponse(RequestResponseChannel listener,
				String request, int timeOut) {
			Integer token = tokenCounter.getAndIncrement();
			String holder[] = new String[1];

			synchronized (holder) {
				requests.put(token, holder);
				listener.send(RequestResponseChannel.REQUEST, token, request);

				while (holder[0] == null)
					Util.wait(holder, timeOut);

				requests.remove(token);
			}

			return holder[0];
		}
	}

	public abstract static class PacketChannel extends BufferedChannel {
		private String received = "";

		public abstract void onReceivePacket(String packet);

		@Override
		public final void onReceive(String message) {
			received += message;

			String packet = receivePacket();
			if (packet != null)
				onReceivePacket(packet);
		}

		protected void sendPacket(String packet) {
			send(strValue(packet.length()) + packet);
		}

		protected String receivePacket() {
			String packet = null;

			if (received.length() >= 4) {
				int end = 4 + intValue(received.substring(0, 4));

				if (received.length() >= end) {
					packet = received.substring(4, end);
					received = received.substring(end);
				}
			}

			return packet;
		}
	}

	public abstract static class BufferedChannel implements ChannelListener {
		private IoProcess<String, String, IOException> sender;
		private String toSend = "";

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
				IoProcess<String, String, IOException> sender) {
			this.sender = sender;
		}

		public void send(String message) {
			toSend += message;

			try {
				trySend();
			} catch (IOException ex) {
				LogUtil.error(getClass(), ex);
			}
		}
	}

	private static String strValue(int i) {
		// return String.format("%04d", i);
		byte bytes[] = new byte[4];
		bytes[0] = (byte) (i & 0xFF);
		bytes[1] = (byte) ((i >>>= 8) & 0xFF);
		bytes[2] = (byte) ((i >>>= 8) & 0xFF);
		bytes[3] = (byte) ((i >>>= 8) & 0xFF);
		String str = new String(bytes, IoUtil.CHARSET);
		return str;
	}

	private static int intValue(String s) {
		// return Integer.valueOf(s);
		byte[] bytes = s.getBytes(IoUtil.CHARSET);
		int length = bytes[3];
		length = (length << 8) + bytes[2];
		length = (length << 8) + bytes[1];
		length = (length << 8) + bytes[0];
		return length;
	}

}
