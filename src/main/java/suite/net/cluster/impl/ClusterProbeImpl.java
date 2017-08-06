package suite.net.cluster.impl;

import java.io.Closeable;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import suite.Constants;
import suite.net.ThreadService;
import suite.net.cluster.ClusterProbe;
import suite.os.LogUtil;
import suite.streamlet.As;
import suite.streamlet.Nerve;
import suite.streamlet.Read;
import suite.util.Object_;
import suite.util.String_;
import suite.util.To;

/**
 * Probes existence of other nodes in a cluster, using the un-reliable UDP
 * protocol (due to its small messaging overhead).
 */
public class ClusterProbeImpl implements ClusterProbe {

	private static int bufferSize = 65536; // uDP packet size
	private static int checkAliveDuration = 1500;
	private static int timeoutDuration = 5000;

	private Selector selector;
	private DatagramChannel channel = DatagramChannel.open();
	private ThreadService threadService = new ThreadService(this::serve);
	private Object lock = new Object(); // lock data structures

	private String me;

	/**
	 * Name/address pairs of all possible peers.
	 */
	private Map<String, IpPort> peers = new HashMap<>();

	/**
	 * Active nodes with their ages.
	 */
	private Map<String, Long> lastActiveTimes = new HashMap<>();

	/**
	 * Time-stamp to avoid HELO bombing.
	 */
	private Map<String, Long> lastSentTimes = new HashMap<>();

	private Nerve<String> onJoined = Nerve.of();
	private Nerve<String> onLeft = Nerve.of();

	private enum Command {
		HELO, FINE, BYEE
	}

	private static class IpPort {
		private byte[] ip;
		private int port;

		private IpPort(InetSocketAddress isa) {
			ip = isa.getAddress().getAddress();
			port = isa.getPort();
		}

		private InetSocketAddress get() throws UnknownHostException {
			return new InetSocketAddress(InetAddress.getByAddress(ip), port);
		}

		@Override
		public boolean equals(Object object) {
			if (Object_.clazz(object) == IpPort.class) {
				IpPort other = (IpPort) object;
				return ip[0] == other.ip[0] //
						&& ip[1] == other.ip[1] //
						&& ip[2] == other.ip[2] //
						&& ip[3] == other.ip[3] //
						&& port == other.port;
			} else
				return false;
		}

		@Override
		public int hashCode() {
			int result = 1;
			result = 31 * result + Arrays.hashCode(ip);
			result = 31 * result + port;
			return result;
		}

		@Override
		public String toString() {
			return Arrays.toString(ip) + ":" + port;
		}
	}

	public ClusterProbeImpl(String me, Map<String, InetSocketAddress> peers) throws IOException {
		this();
		this.me = me;
		setPeers(peers);
	}

	public ClusterProbeImpl() throws IOException {
		channel.configureBlocking(false);
	}

	@Override
	public synchronized void start() {
		synchronized (lock) {
			lastActiveTimes.put(me, System.currentTimeMillis());
			broadcast(Command.HELO);
		}
		threadService.start();
	}

	@Override
	public synchronized void stop() {
		threadService.stop();
		synchronized (lock) {
			broadcast(Command.BYEE);
			lastActiveTimes.clear();
		}
	}

	private void serve() throws IOException {
		InetSocketAddress address = peers.get(me).get();

		selector = Selector.open();

		DatagramChannel dc = DatagramChannel.open();
		dc.configureBlocking(false);
		dc.socket().bind(address);
		dc.register(selector, SelectionKey.OP_READ);

		try (Closeable started = threadService.started()) {
			while (threadService.isRunning()) {
				selector.select(500); // handle network events

				synchronized (lock) {
					long current = System.currentTimeMillis();
					nodeJoined(me, current);
					processSelectedKeys();
					keepAlive(current);
					eliminateOutdatedPeers(current);
				}
			}

			for (String peer : lastActiveTimes.keySet())
				onLeft.fire(peer);
		}

		dc.close();
		selector.close();
	}

	private void processSelectedKeys() {
		Iterator<SelectionKey> keyIter = selector.selectedKeys().iterator();
		while (keyIter.hasNext()) {
			SelectionKey key = keyIter.next();
			keyIter.remove();

			try {
				processSelectedKey(key);
			} catch (Exception ex) {
				LogUtil.error(ex);
			}
		}
	}

	private void processSelectedKey(SelectionKey key) throws IOException {
		DatagramChannel dc = (DatagramChannel) key.channel();

		if (key.isReadable()) {
			ByteBuffer buffer = ByteBuffer.allocate(bufferSize);
			dc.receive(buffer);
			buffer.flip();

			byte[] bytes = new byte[buffer.remaining()];
			buffer.get(bytes);
			buffer.rewind();

			String[] splitted = To.string(bytes).split(",");
			Command data = Command.valueOf(splitted[0]);
			String remote = splitted[1];

			// refreshes member time accordingly
			for (int i = 2; i < splitted.length; i += 2) {
				String node = splitted[i];
				long newTime = Long.parseLong(splitted[i + 1]);
				nodeJoined(node, newTime);
			}

			if (peers.get(remote) != null)
				if (data == Command.HELO) // reply HELO messages
					sendMessage(remote, formMessage(Command.FINE));
				else if (data == Command.BYEE && lastActiveTimes.remove(remote) != null)
					onLeft.fire(remote);
		}
	}

	private void nodeJoined(String node, long time) {
		Long oldTime = lastActiveTimes.get(node);

		if (oldTime == null || oldTime < time)
			if (lastActiveTimes.put(node, time) == null)
				onJoined.fire(node);
	}

	private void keepAlive(long current) {
		byte[] bytes = formMessage(Command.HELO);

		for (String remote : peers.keySet()) {
			Long lastActive = lastActiveTimes.get(remote);
			Long lastSent = lastSentTimes.get(remote);

			// sends to those who are nearly forgotten, i.e.:
			// - The node is not active, or node's active time is expired
			// - The last sent time was long ago (avoid message bombing)
			if (lastActive == null || lastActive + checkAliveDuration < current)
				if (lastSent == null || lastSent + checkAliveDuration < current)
					sendMessage(remote, bytes);
		}
	}

	private void eliminateOutdatedPeers(long current) {
		Set<Entry<String, Long>> entries = lastActiveTimes.entrySet();
		Iterator<Entry<String, Long>> peerIter = entries.iterator();

		while (peerIter.hasNext()) {
			Entry<String, Long> e = peerIter.next();
			String node = e.getKey();

			if (timeoutDuration < current - e.getValue()) {
				peerIter.remove();
				onLeft.fire(node);
			}
		}
	}

	/**
	 * Sends message to all nodes.
	 *
	 * TODO this is costly and un-scalable.
	 */
	private void broadcast(Command data) {
		byte[] bytes = formMessage(data);

		for (String remote : peers.keySet())
			if (!String_.equals(remote, me))
				sendMessage(remote, bytes);
	}

	private void sendMessage(String remote, byte[] bytes) {
		try {
			channel.send(ByteBuffer.wrap(bytes), peers.get(remote).get());
			lastSentTimes.put(remote, System.currentTimeMillis());
		} catch (IOException ex) {
			LogUtil.error(ex);
		}
	}

	private byte[] formMessage(Command data) {
		StringBuilder sb = new StringBuilder(data.name() + "," + me);

		for (Entry<String, Long> e : lastActiveTimes.entrySet())
			sb.append("," + e.getKey() + "," + e.getValue());

		return sb.toString().getBytes(Constants.charset);
	}

	@Override
	public boolean isActive(String node) {
		return lastActiveTimes.containsKey(node);
	}

	@Override
	public Set<String> getActivePeers() {
		return Collections.unmodifiableSet(lastActiveTimes.keySet());
	}

	@Override
	public String toString() {
		return Read.from2(lastActiveTimes) //
				.map((peer, lastActiveTime) -> peer + " (last-active = " + To.string(lastActiveTime) + ")") //
				.collect(As.conc("\n"));
	}

	private void setPeers(Map<String, InetSocketAddress> peers) {
		for (Entry<String, InetSocketAddress> e : peers.entrySet())
			this.peers.put(e.getKey(), new IpPort(e.getValue()));
	}

	public Nerve<String> getOnJoined() {
		return onJoined;
	}

	public Nerve<String> getOnLeft() {
		return onLeft;
	}

}
