package org.net;

import java.io.Closeable;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ThreadPoolExecutor;

import org.net.Channels.PersistableChannel;
import org.util.FunUtil.Fun;
import org.util.FunUtil.Sink;
import org.util.FunUtil.Sinks;
import org.util.FunUtil.Source;
import org.util.Util;

public class Cluster {

	private String me;
	private Map<String, InetSocketAddress> peers;
	private ClusterProbe probe;

	private NioDispatcher<ClusterChannel> nio = new NioDispatcher<>(new Source<ClusterChannel>() {
		public ClusterChannel apply() {
			return new ClusterChannel(me);
		}
	});

	private RequestResponseMatcher matcher = new RequestResponseMatcher();
	private ThreadPoolExecutor executor = Util.createExecutor();

	private Closeable unlisten;

	/**
	 * Established channels connecting to peers.
	 */
	private Map<String, ClusterChannel> channels = new HashMap<>();

	private Sinks<String> onJoined = new Sinks<>();
	private Sinks<String> onLeft = new Sinks<>();
	private Map<Class<?>, Fun<?, ?>> onReceive = new HashMap<>();

	public static class ClusterException extends RuntimeException {
		private static final long serialVersionUID = 1L;

		private ClusterException(Throwable cause) {
			super(cause);
		}
	}

	private class ClusterChannel extends PersistableChannel<ClusterChannel> {
		private ClusterChannel(String peer) {
			super(nio, matcher, executor, peers.get(peer), new Fun<Bytes, Bytes>() {
				public Bytes apply(Bytes request) {
					return Cluster.this.respondToRequest(request);
				}
			});
		}
	}

	public Cluster(final String me, Map<String, InetSocketAddress> peers) throws IOException {
		this.me = me;
		this.peers = peers;
		probe = new ClusterProbe(me, peers);
	}

	public void start() throws IOException {
		probe.setOnJoined(new Sink<String>() {
			public void apply(String node) {
				onJoined.apply(node);
			}
		});

		probe.setOnLeft(new Sink<String>() {
			public void apply(String node) {
				ClusterChannel channel = channels.get(node);

				if (channel != null)
					channel.stop();

				onLeft.apply(node);
			}
		});

		unlisten = nio.listen(peers.get(me).getPort());
		nio.start();
		probe.start();
	}

	public void stop() {
		for (ClusterChannel channel : channels.values())
			channel.stop();

		probe.stop();
		nio.stop();
		Util.closeQuietly(unlisten);
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
