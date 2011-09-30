package org.net;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.net.NioServer.ChannelListener;
import org.util.Util;
import org.util.Util.Event;

public abstract class ChannelListeners implements ChannelListener {

	public abstract static class RequestResponseChannel extends PacketChannel {
		private RequestResponseMatcher matcher;
		private boolean connected;

		public RequestResponseChannel(RequestResponseMatcher matcher) {
			this.matcher = matcher;
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
				int token = intValue(packet.substring(1, 5));

				packet = packet.substring(5);

				if (type == 'P') // Response
					matcher.onRespond(token, packet);
				else if (type == 'Q')
					send('P', token, respondForRequest(packet));
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

		// TODO clean-up obsoletes
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
				listener.send('Q', token, request);

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
		private Event eventForSend;
		private String toSend = "";

		@Override
		public void onConnected() {
		}

		@Override
		public void onClose() {
		}

		@Override
		public void onSent(int size) {
			toSend = toSend.substring(size);

			if (!toSend.isEmpty())
				eventForSend.perform(null);
		}

		@Override
		public void setEventForSend(Event event) {
			this.eventForSend = event;
		}

		@Override
		public String getMessageToSend() {
			return toSend;
		}

		public void send(String message) {
			toSend += message;
			eventForSend.perform(null);
		}
	}

	private static String strValue(int i) {
		// return String.format("%04d", i);
		byte bytes[] = new byte[4];
		bytes[0] = (byte) (i & 0xFF);
		bytes[1] = (byte) ((i >>>= 8) & 0xFF);
		bytes[2] = (byte) ((i >>>= 8) & 0xFF);
		bytes[3] = (byte) ((i >>>= 8) & 0xFF);
		String str = new String(bytes);
		return str;
	}

	private static int intValue(String s) {
		// return Integer.valueOf(s);
		byte[] bytes = s.getBytes();
		int length = bytes[3];
		length = (length << 8) + bytes[2];
		length = (length << 8) + bytes[1];
		length = (length << 8) + bytes[0];
		return length;
	}

}
