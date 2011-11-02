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
import org.util.Util.MultiSetter;
import org.util.Util.Setter;
import org.util.Util.Transformer;

public class Cluster {

	private String me;
	private Map<String, InetSocketAddress> peers;

	private ClusterProbe probe;

	private NioDispatcher<PersistableChannel> nio;
	private RequestResponseMatcher matcher = new RequestResponseMatcher();
	private ThreadPoolExecutor executor = Util.createExecutor();

	/**
	 * Established channels connecting to peers.
	 */
	private Map<String, ClusterChannel> channels;

	private MultiSetter<String> onJoined = Util.multiSetter();
	private MultiSetter<String> onLeft = Util.multiSetter();
	private Map<Class<?>, Transformer<?, ?>> onReceive = new HashMap<Class<?>, Transformer<?, ?>>();

	private final class ClusterChannel extends PersistableChannel {
		private ClusterChannel() {
			super(nio, matcher, executor, peers.get(me));
		}

		public String respondToRequest(String request) {
			return Cluster.this.respondToRequest(request);
		}
	}

	public Cluster(String me, Map<String, InetSocketAddress> peers)
			throws IOException {
		probe = new ClusterProbe(me, peers);
		nio = new NioDispatcher<PersistableChannel>(
				new ChannelListenerFactory<PersistableChannel>() {
					public PersistableChannel create() {
						return new ClusterChannel();
					}
				});
	}

	public void start() {
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

		probe.spawn();
	}

	public void stop() {
		probe.unspawn();
	}

	public String requestForResponse(String peer, Object request) {
		if (probe.isActive(peer))
			return matcher.requestForResponse(getChannel(peer),
					NetUtil.serialize(request));
		else
			throw new RuntimeException("Peer " + peer + " is not active");
	}

	private ClusterChannel getChannel(String peer) {
		ClusterChannel channel = channels.get(peer);
		if (channel == null)
			channels.put(peer, channel = new ClusterChannel());
		return channel;
	}

	private String respondToRequest(String m) {
		Object request = NetUtil.deserialize(m);
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
