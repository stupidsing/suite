package org.net;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.Map;

import org.util.Util.Setter;

public class Cluster {

	private ClusterProbe probe;

	public Cluster(String me, Map<String, InetSocketAddress> peers)
			throws IOException {
		probe = new ClusterProbe(me, peers);
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

}
