package org.net;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.Map;
import java.util.concurrent.ThreadPoolExecutor;

import org.net.ChannelListeners.PersistableChannel;
import org.net.ChannelListeners.RequestResponseMatcher;
import org.net.NioDispatcher.ChannelListenerFactory;
import org.util.Util;
import org.util.Util.Pair;
import org.util.Util.Setter;

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

	private final class ClusterChannel extends PersistableChannel {
		private ClusterChannel() {
			super(nio, matcher, executor, peers.get(me));
		}

		public String respondForRequest(String request) {
			return Cluster.this.respondForRequest(request);
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
				return null;
			}
		});

		probe.setOnLeft(new Setter<String>() {
			public Void perform(String node) {
				return null;
			}
		});

		probe.spawn();
	}

	public void stop() {
		probe.unspawn();
	}

	public void sendAll(String message) {
		for (String peer : probe.getActivePeers())
			sendTo(peer, message);
	}

	public void sendTo(String peer, String message) {
		ClusterChannel channel = channels.get(peer);
		if (channel == null)
			channels.put(peer, channel = new ClusterChannel());

		channel.send(message);
	}

	private String respondForRequest(String request) {
		return request;
	}

	public void setOnReceive(Setter<Pair<String, String>> delegate) {
	}

}
