package org.net;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.util.Arrays;
import java.util.Date;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.util.FormatUtil;
import org.util.LogUtil;
import org.util.Util;
import org.util.Util.Setter;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;

public class ClusterProbe extends ThreadedService {

	private final static int BUFFERSIZE = 65536;
	private final static int CHECKALIVEDURATION = 1500;
	private final static int TIMEOUTDURATION = 5000;

	private Selector selector;
	private DatagramChannel channel = DatagramChannel.open();
	private ByteBuffer buffer = ByteBuffer.allocate(BUFFERSIZE);

	private String me;
	private BiMap<String, Address> peers = HashBiMap.create();
	private Map<String, Long> lastActiveTime = Util.createHashMap();

	private Setter<String> onJoined = Util.nullSetter();
	private Setter<String> onLeft = Util.nullSetter();

	private enum Command {
		HELO, BYEE
	}

	private final static int COMMANDLENGTH = Command.HELO.name().length();

	private static class Address {
		private byte ip[];
		private int port;

		private Address(InetSocketAddress isa) {
			ip = isa.getAddress().getAddress();
			port = isa.getPort();
		}

		private InetSocketAddress get() throws UnknownHostException {
			return new InetSocketAddress(InetAddress.getByAddress(ip), port);
		}

		@Override
		public int hashCode() {
			int result = 1;
			result = 31 * result + Arrays.hashCode(ip);
			result = 31 * result + port;
			return result;
		}

		@Override
		public boolean equals(Object object) {
			if (object instanceof Address) {
				Address other = (Address) object;
				return ip[0] == other.ip[0] //
						&& ip[1] == other.ip[1] //
						&& ip[2] == other.ip[2] //
						&& ip[3] == other.ip[3] //
						&& port == other.port;
			} else
				return false;
		}

		@Override
		public String toString() {
			return Arrays.toString(ip) + ":" + port;
		}
	}

	public ClusterProbe(String me, Map<String, InetSocketAddress> peers)
			throws IOException {
		this.me = me;
		for (Entry<String, InetSocketAddress> e : peers.entrySet())
			this.peers.put(e.getKey(), new Address(e.getValue()));
		channel.configureBlocking(false);
	}

	@Override
	public synchronized void spawn() {
		lastActiveTime.put(me, System.currentTimeMillis()); // Puts myself in
		super.spawn();

	}

	@Override
	public synchronized void unspawn() {
		super.unspawn();
		broadcast(Command.BYEE);
		lastActiveTime.clear();
	}

	public String dumpActivePeers() {
		StringBuilder sb = new StringBuilder();
		for (Entry<String, Long> e : lastActiveTime.entrySet()) {
			String dateStr = FormatUtil.dtFmt.format(new Date(e.getValue()));
			sb.append(e.getKey() + " (last-active = " + dateStr + ")\n");
		}
		return sb.toString();
	}

	protected void serve() throws IOException {
		InetSocketAddress address = peers.get(me).get();

		selector = Selector.open();

		final DatagramChannel dc = DatagramChannel.open();
		dc.configureBlocking(false);
		dc.socket().bind(address);
		dc.register(selector, SelectionKey.OP_READ);

		setStarted(true);
		long broadcastSentTime = System.currentTimeMillis();

		while (running) {

			// Sends keep-alive
			long current = System.currentTimeMillis();
			if (current - broadcastSentTime > CHECKALIVEDURATION) {
				broadcast(Command.HELO);
				broadcastSentTime = current;
			}

			// Eliminate peers that passed out
			Set<Entry<String, Long>> entries = lastActiveTime.entrySet();
			Iterator<Entry<String, Long>> peerIter = entries.iterator();
			while (peerIter.hasNext()) {
				Entry<String, Long> e = peerIter.next();
				String name = e.getKey();

				if (!Util.equals(name, me)
						&& current - e.getValue() > TIMEOUTDURATION) {
					peerIter.remove();
					onLeft.perform(name);
				}
			}

			// Handle network events
			selector.select(500);

			Iterator<SelectionKey> keyIter = selector.selectedKeys().iterator();
			while (keyIter.hasNext()) {
				SelectionKey key = keyIter.next();
				keyIter.remove();

				try {
					processSelectedKey(current, key);
				} catch (Exception ex) {
					LogUtil.error(getClass(), ex);
				}
			}
		}

		setStarted(false);

		dc.close();
		selector.close();
	}

	private void processSelectedKey(long current, SelectionKey key)
			throws IOException {
		DatagramChannel dc = (DatagramChannel) key.channel();

		if (key.isReadable()) {
			dc.receive(buffer);
			buffer.flip();

			byte bytes[] = new byte[buffer.remaining()];
			buffer.get(bytes);
			buffer.rewind();

			String message = new String(bytes);
			Command data = Command.valueOf(message.substring(0, COMMANDLENGTH));
			String remoteName = message.substring(COMMANDLENGTH);

			if (peers.get(remoteName) != null)
				if (data == Command.HELO
						&& lastActiveTime.put(remoteName, current) == null)
					onJoined.perform(remoteName);
				else if (data == Command.BYEE
						&& lastActiveTime.remove(remoteName) != null)
					onLeft.perform(remoteName);
		}
	}

	private void broadcast(Command data) {
		byte[] bytes = (data.name() + me).getBytes();

		for (Entry<String, Address> e : peers.entrySet())
			if (!Util.equals(e.getKey(), me))
				try {
					channel.send(ByteBuffer.wrap(bytes), e.getValue().get());
				} catch (IOException ex) {
					LogUtil.error(getClass(), ex);
				}
	}

	public Set<String> getActivePeers() {
		return lastActiveTime.keySet();
	}

	public void setOnJoined(Setter<String> onJoined) {
		this.onJoined = onJoined;
	}

	public void setOnLeft(Setter<String> onLeft) {
		this.onLeft = onLeft;
	}

}
