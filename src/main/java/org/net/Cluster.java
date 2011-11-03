package org.net;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ThreadPoolExecutor;

import org.net.ChannelListeners.PersistableChannel;
import org.net.ChannelListeners.RequestResponseMatcher;
import org.net.NioDispatcher.ChannelListenerFactory;
import org.util.Util;
import org.util.Util.Event;
import org.util.Util.MultiSetter;
import org.util.Util.Setter;
import org.util.Util.Transformer;

public class Cluster {

	private String me;
	private Map<String, InetSocketAddress> peers;

	private ClusterProbe probe;

	private NioDispatcher<ClusterChannel> nio;
	private RequestResponseMatcher matcher = new RequestResponseMatcher();
	private ThreadPoolExecutor executor = Util.createExecutor();

	private Event unlisten;

	/**
	 * Established channels connecting to peers.
	 */
	private Map<String, ClusterChannel> channels = Util.createHashMap();

	private MultiSetter<String> onJoined = Util.multiSetter();
	private MultiSetter<String> onLeft = Util.multiSetter();
	private Map<Class<?>, Transformer<?, ?>> onReceive = new HashMap<Class<?>, Transformer<?, ?>>();

	public static class ClusterException extends RuntimeException {
		private static final long serialVersionUID = 1L;

		private ClusterException(Throwable cause) {
			super(cause);
		}
	}

	private final class ClusterChannel extends
			PersistableChannel<ClusterChannel> {
		private ClusterChannel(String peer) {
			super(nio, matcher, executor, peers.get(peer));
		}

		public String respondToRequest(String request) {
			return Cluster.this.respondToRequest(request);
		}
	}

	public Cluster(final String me, Map<String, InetSocketAddress> peers)
			throws IOException {
		this.me = me;
		this.peers = peers;
		probe = new ClusterProbe(me, peers);

		nio = new NioDispatcher<ClusterChannel>(
				new ChannelListenerFactory<ClusterChannel>() {
					public ClusterChannel create() {
						return new ClusterChannel(me);
					}
				});
	}

	public void start() throws IOException {
		probe.setOnJoined(new Setter<String>() {
			public Void perform(String node) {
				return Cluster.this.onJoined.perform(node);
			}
		});

		probe.setOnLeft(new Setter<String>() {
			public Void perform(String node) {
				ClusterChannel channel = channels.get(node);

				if (channel != null)
					channel.stop();

				return Cluster.this.onLeft.perform(node);
			}
		});

		unlisten = nio.listen(peers.get(me).getPort());
		nio.spawn();
		probe.spawn();
	}

	public void stop() {
		for (ClusterChannel channel : channels.values())
			channel.stop();

		nio.unspawn();
		probe.unspawn();
		unlisten.perform(null);
	}

	public Object requestForResponse(String peer, Object request) {
		if (probe.isActive(peer)) {
			String req = NetUtil.serialize(request);
			String resp = matcher.requestForResponse(getChannel(peer), req);
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

	private String respondToRequest(String req) {
		Object request = NetUtil.deserialize(req);
		@SuppressWarnings("unchecked")
		Transformer<Object, Object> handler = (Transformer<Object, Object>) onReceive
				.get(request.getClass());
		return NetUtil.serialize(handler.perform(request));
	}

	public void addOnJoined(Setter<String> onJoined) {
		this.onJoined.add(onJoined);
	}

	public void addOnLeft(Setter<String> onLeft) {
		this.onLeft.add(onLeft);
	}

	public <I, O> void setOnReceive(Class<I> clazz, Transformer<I, O> onReceive) {
		this.onReceive.put(clazz, onReceive);
	}

}
