package org.net;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.Map;
import java.util.concurrent.ThreadPoolExecutor;

import org.net.ChannelListeners.PersistableChannel;
import org.net.ChannelListeners.RequestResponseMatcher;
import org.net.NioDispatcher.ChannelListenerFactory;
import org.util.Util;
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

	private Transformer<String, String> onReceive;

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
				return null;
			}
		});

		probe.setOnLeft(new Setter<String>() {
			public Void perform(String node) {
				ClusterChannel channel = channels.get(node);

				if (channel != null)
					channel.stop();

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

	public boolean sendTo(String peer, String message) {
		if (probe.isActive(peer)) {
			ClusterChannel channel = channels.get(peer);

			if (channel == null)
				channels.put(peer, channel = new ClusterChannel());

			channel.send(message);
			return true;
		} else
			return false;
	}

	private String respondToRequest(String request) {
		return onReceive.perform(request);
	}

	public void setOnReceive(Transformer<String, String> onReceive) {
		this.onReceive = onReceive;
	}

}
