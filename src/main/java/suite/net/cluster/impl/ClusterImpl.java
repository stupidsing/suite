package suite.net.cluster.impl;

import java.io.Closeable;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ThreadPoolExecutor;

import suite.net.NetUtil;
import suite.net.NioDispatcher;
import suite.net.NioDispatcherImpl;
import suite.net.RequestResponseMatcher;
import suite.net.channels.PersistentChannel;
import suite.net.cluster.Cluster;
import suite.net.cluster.ClusterProbe;
import suite.primitive.Bytes;
import suite.streamlet.Reactive;
import suite.util.FunUtil.Fun;
import suite.util.Util;

public class ClusterImpl implements Cluster {

	private String me;
	private Map<String, InetSocketAddress> peers;
	private ClusterProbe probe;

	private NioDispatcher<PersistentChannel> nio = new NioDispatcherImpl<>(() -> new ClusterChannel(me));

	private RequestResponseMatcher matcher = new RequestResponseMatcher();
	private ThreadPoolExecutor executor;

	private Closeable unlisten;

	/**
	 * Established channels connecting to peers.
	 */
	private Map<String, PersistentChannel> channels = new HashMap<>();

	private Reactive<String> onJoined;
	private Reactive<String> onLeft;
	private Map<Class<?>, Fun<?, ?>> onReceive = new HashMap<>();

	private class ClusterChannel extends PersistentChannel {
		private ClusterChannel(String peer) {
			super(nio, matcher, executor, peers.get(peer), ClusterImpl.this::respondToRequest);
		}
	}

	public ClusterImpl(String me, Map<String, InetSocketAddress> peers) throws IOException {
		this.me = me;
		this.peers = peers;
		probe = new ClusterProbeImpl(me, peers);
	}

	@Override
	public void start() throws IOException {
		executor = Util.createExecutor();

		unlisten = nio.listen(peers.get(me).getPort());
		nio.start();

		onJoined = probe.getOnJoined();

		onLeft = probe.getOnLeft().map(node -> {
			PersistentChannel channel = channels.get(node);
			if (channel != null)
				channel.stop();
			return node;
		});

		probe.start();
	}

	@Override
	public void stop() {
		for (PersistentChannel channel : channels.values())
			channel.stop();

		probe.stop();
		nio.stop();
		Util.closeQuietly(unlisten);

		executor.shutdown();
	}

	@Override
	public Object requestForResponse(String peer, Object request) {
		if (probe.isActive(peer)) {
			Bytes req = NetUtil.serialize(request);
			Bytes resp = matcher.requestForResponse(getChannel(peer), req);
			return NetUtil.deserialize(resp);
		} else
			throw new RuntimeException("Peer " + peer + " is not active");
	}

	private PersistentChannel getChannel(String peer) {
		PersistentChannel channel = channels.get(peer);

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

	@Override
	public <I, O> void setOnReceive(Class<I> clazz, Fun<I, O> onReceive) {
		this.onReceive.put(clazz, onReceive);
	}

	@Override
	public Set<String> getActivePeers() {
		return probe.getActivePeers();
	}

	@Override
	public Reactive<String> getOnJoined() {
		return onJoined;
	}

	@Override
	public Reactive<String> getOnLeft() {
		return onLeft;
	}

	@Override
	public String getMe() {
		return me;
	}

}
