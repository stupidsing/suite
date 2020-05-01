package suite.net.cluster.impl;

import primal.MoreVerbs.Read;
import primal.Nouns.Utf8;
import primal.Verbs.Equals;
import primal.Verbs.Format;
import primal.os.Log_;
import primal.primitive.adt.pair.IntIntPair;
import suite.net.NetUtil;
import suite.net.ThreadService;
import suite.net.cluster.ClusterProbe;
import suite.streamlet.As;
import suite.streamlet.Pusher;
import suite.util.To;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.util.*;

import static primal.statics.Rethrow.ex;

/**
 * Probes existence of other nodes in a cluster, using the un-reliable UDP
 * protocol (less messaging overhead).
 */
public class ClusterProbeImpl implements ClusterProbe {

	private static int bufferSize = 65536; // UDP packet size
	private static int checkAliveDuration = 1500;
	private static int timeoutDuration = 5000;

	private Recv recv = new Recv();
	private Send send = new Send();
	private ThreadService threadService = new ThreadService(recv::serve);
	private Object lock = new Object(); // lock data structures

	private String me;

	/**
	 * Name/address pairs of all possible peers.
	 */
	private Map<String, IpPort> peers = new HashMap<>();

	/**
	 * Active nodes with their ages.
	 */
	private Map<String, Long> lastActiveTimeByPeer = new HashMap<>();

	/**
	 * Time-stamp to avoid HELO bombing.
	 */
	private Map<String, Long> lastSentTimeByPeer = new HashMap<>();

	private Pusher<String> onJoined = new Pusher<>();
	private Pusher<String> onLeft = new Pusher<>();

	private enum Command {
		HELO, FINE, BYEE
	}

	private static class IpPort extends IntIntPair {
		private IpPort(InetSocketAddress isa) {
			super(NetUtil.bsToInt(isa.getAddress().getAddress()), isa.getPort());
		}

		private InetSocketAddress get() throws UnknownHostException {
			return new InetSocketAddress(InetAddress.getByAddress(ip()), port());
		}

		@Override
		public String toString() {
			return Arrays.toString(ip()) + ":" + port();
		}

		private byte[] ip() {
			return NetUtil.intToBs(t0);
		}

		private int port() {
			return t1;
		}
	}

	public ClusterProbeImpl(String me, Map<String, InetSocketAddress> peers) throws IOException {
		this.me = me;
		setPeers(peers);
	}

	@Override
	public synchronized void start() {
		synchronized (lock) {
			lastActiveTimeByPeer.put(me, System.currentTimeMillis());
			send.broadcast(Command.HELO);
		}
		threadService.start();
	}

	@Override
	public synchronized void stop() {
		threadService.stop();
		synchronized (lock) {
			send.broadcast(Command.BYEE);
			lastActiveTimeByPeer.clear();
		}
	}

	private class Recv {
		private void serve() throws IOException {
			var address = peers.get(me).get();

			try (var selector = Selector.open();
					var dc = DatagramChannel.open();
					var started = threadService.started()) {
				dc.configureBlocking(false);
				dc.socket().bind(address);
				dc.register(selector, SelectionKey.OP_READ);

				while (threadService.isRunning()) {
					selector.select(500); // handle network events

					synchronized (lock) {
						var current = System.currentTimeMillis();
						processSelectedKeys(selector);
						heartbeat(current);
					}
				}

				for (var peer : lastActiveTimeByPeer.keySet())
					onLeft.push(peer);
			}
		}

		private void processSelectedKeys(Selector selector) {
			var iter = selector.selectedKeys().iterator();

			while (iter.hasNext()) {
				var key = iter.next();
				iter.remove();

				try {
					processSelectedKey(key);
				} catch (Exception ex) {
					Log_.error(ex);
				}
			}
		}

		private void processSelectedKey(SelectionKey key) throws IOException {
			var dc = (DatagramChannel) key.channel();

			if (key.isReadable()) {
				var buffer = ByteBuffer.allocate(bufferSize);
				dc.receive(buffer);
				buffer.flip();

				var bytes = new byte[buffer.remaining()];
				buffer.get(bytes);
				buffer.rewind();

				processDatagram(bytes);
			}
		}
	}

	private class Send {
		private DatagramChannel channel;

		private Send() {
			ex(() -> {
				channel = DatagramChannel.open();
				return channel.configureBlocking(false);
			});
		}

		private void keepAlive(long current) {
			var bytes = formMessage(Command.HELO);

			for (var remote : peers.keySet()) {
				var lastActive = lastActiveTimeByPeer.get(remote);
				var lastSent = lastSentTimeByPeer.get(remote);

				// sends to those who are nearly forgotten, i.e.:
				// - The node is not active, or node's active time is expired
				// - The last sent time was long ago (avoid message bombing)
				if (lastActive == null || lastActive + checkAliveDuration < current)
					if (lastSent == null || lastSent + checkAliveDuration < current)
						sendMessage(remote, bytes);
			}
		}

		/**
		 * Sends message to all nodes.
		 *
		 * TODO this is costly and un-scalable.
		 */
		private void broadcast(Command data) {
			var bytes = formMessage(data);

			for (var remote : peers.keySet())
				if (!Equals.string(remote, me))
					sendMessage(remote, bytes);
		}

		private void sendMessage(String remote, byte[] bytes) {
			try {
				channel.send(ByteBuffer.wrap(bytes), peers.get(remote).get());
				lastSentTimeByPeer.put(remote, System.currentTimeMillis());
			} catch (IOException ex) {
				Log_.error(ex);
			}
		}
	}

	private void heartbeat(long current) {
		nodeJoined(me, current);
		send.keepAlive(current);
		eliminateOutdatedPeers(current);
	}

	private void processDatagram(byte[] bytes) {
		var splitted = To.string(bytes).split(",");
		var data = Command.valueOf(splitted[0]);
		var remote = splitted[1];

		// refreshes member time accordingly
		for (var i = 2; i < splitted.length; i += 2) {
			var node = splitted[i];
			var newTime = Long.parseLong(splitted[i + 1]);
			nodeJoined(node, newTime);
		}

		if (peers.get(remote) != null)
			if (data == Command.HELO) // reply HELO messages
				send.sendMessage(remote, formMessage(Command.FINE));
			else if (data == Command.BYEE && lastActiveTimeByPeer.remove(remote) != null)
				onLeft.push(remote);
	}

	private void nodeJoined(String node, long time) {
		var oldTime = lastActiveTimeByPeer.get(node);

		if (oldTime == null || oldTime < time)
			if (lastActiveTimeByPeer.put(node, time) == null)
				onJoined.push(node);
	}

	private void eliminateOutdatedPeers(long current) {
		var entries = lastActiveTimeByPeer.entrySet();
		var peerIter = entries.iterator();

		while (peerIter.hasNext()) {
			var e = peerIter.next();
			var node = e.getKey();

			if (timeoutDuration < current - e.getValue()) {
				peerIter.remove();
				onLeft.push(node);
			}
		}
	}

	private byte[] formMessage(Command data) {
		return Read
				.from2(lastActiveTimeByPeer)
				.map((peer, lastActiveTime) -> "," + peer + "," + lastActiveTime)
				.cons(data.name() + "," + me)
				.toJoinedString()
				.getBytes(Utf8.charset);
	}

	@Override
	public boolean isActive(String node) {
		return lastActiveTimeByPeer.containsKey(node);
	}

	@Override
	public Set<String> getActivePeers() {
		return Collections.unmodifiableSet(lastActiveTimeByPeer.keySet());
	}

	@Override
	public String toString() {
		return Read
				.from2(lastActiveTimeByPeer)
				.map((peer, lastActiveTime) -> peer + " (last-active = " + Format.ymdHms(lastActiveTime) + ")")
				.collect(As.conc("\n"));
	}

	private void setPeers(Map<String, InetSocketAddress> peers1) {
		this.peers.putAll(Read.from2(peers1).mapValue(IpPort::new).toMap());
	}

	public Pusher<String> getOnJoined() {
		return onJoined;
	}

	public Pusher<String> getOnLeft() {
		return onLeft;
	}

}
