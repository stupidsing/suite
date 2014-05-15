package suite.net.cluster;

import java.io.Closeable;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ThreadPoolExecutor;

import suite.net.NetUtil;
import suite.net.NioDispatcher;
import suite.net.RequestResponseMatcher;
import suite.net.channels.PersistableChannel;
import suite.primitive.Bytes;
import suite.util.FunUtil.Fun;
import suite.util.FunUtil.Sink;
import suite.util.FunUtil.Sinks;
import suite.util.Util;

public class Cluster {

	private String me;
	private Map<String, InetSocketAddress> peers;
	private ClusterProbe probe;

	private NioDispatcher<ClusterChannel> nio = new NioDispatcher<>(() -> new ClusterChannel(me));

	private RequestResponseMatcher matcher = new RequestResponseMatcher();
	private ThreadPoolExecutor executor;

	private Closeable unlisten;

	/**
	 * Established channels connecting to peers.
	 */
	private Map<String, ClusterChannel> channels = new HashMap<>();

	private Sinks<String> onJoined = new Sinks<>();
	private Sinks<String> onLeft = new Sinks<>();
	private Map<Class<?>, Fun<?, ?>> onReceive = new HashMap<>();

	public static class ClusterException extends RuntimeException {
		private static final long serialVersionUID = 1;

		private ClusterException(Throwable cause) {
			super(cause);
		}
	}

	private class ClusterChannel extends PersistableChannel<ClusterChannel> {
		private ClusterChannel(String peer) {
			super(nio, matcher, executor, peers.get(peer), new Fun<Bytes, Bytes>() {
				public Bytes apply(Bytes request) {
					return respondToRequest(request);
				}
			});
		}
	}

	public Cluster(String me, Map<String, InetSocketAddress> peers) throws IOException {
		this.me = me;
		this.peers = peers;
		probe = new ClusterProbe(me, peers);
	}

	public void start() throws IOException {
		executor = Util.createExecutor();

		unlisten = nio.listen(peers.get(me).getPort());
		nio.start();

		probe.setOnJoined(onJoined::sink);

		probe.setOnLeft(node -> {
			ClusterChannel channel = channels.get(node);
			if (channel != null)
				channel.stop();
			onLeft.sink(node);
		});

		probe.start();
	}

	public void stop() {
		for (ClusterChannel channel : channels.values())
			channel.stop();

		probe.stop();
		nio.stop();
		Util.closeQuietly(unlisten);

		executor.shutdown();
	}

	public Object requestForResponse(String peer, Object request) {
		if (probe.isActive(peer)) {
			Bytes req = NetUtil.serialize(request);
			Bytes resp = matcher.requestForResponse(getChannel(peer), req);
			return NetUtil.deserialize(resp);
		} else
			throw new RuntimeException("Peer " + peer + " is not active");
	}

	private ClusterChannel getChannel(String peer) {
		ClusterChannel channel = channels.get(peer);

		if (channel == null || !channel.isConnected())
			try {
				if (channel != null)
					nio.disconnect(channel);

				channel = nio.connect(peers.get(peer));
				channels.put(peer, channel);
			} catch (IOException ex) {
				throw new ClusterException(ex);
			}

		return channel;
	}

	private Bytes respondToRequest(Bytes req) {
		Object request = NetUtil.deserialize(req);
		@SuppressWarnings("unchecked")
		Fun<Object, Object> handler = (Fun<Object, Object>) onReceive.get(request.getClass());
		return NetUtil.serialize(handler.apply(request));
	}

	public void addOnJoined(Sink<String> onJoined) {
		this.onJoined.add(onJoined);
	}

	public void addOnLeft(Sink<String> onLeft) {
		this.onLeft.add(onLeft);
	}

	public <I, O> void setOnReceive(Class<I> clazz, Fun<I, O> onReceive) {
		this.onReceive.put(clazz, onReceive);
	}

	public String getMe() {
		return me;
	}

	public Set<String> getActivePeers() {
		return probe.getActivePeers();
	}

}
