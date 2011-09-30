package org.net;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.Map;
import java.util.Map.Entry;

import org.junit.Test;
import org.util.Util;

public class ClusterProbeTest {

	@Test
	public void test() throws IOException {
		InetAddress localHost = InetAddress.getLocalHost();

		Map<String, InetSocketAddress> peers = Util.createHashMap();
		peers.put("NODE0", new InetSocketAddress(localHost, 3000));
		peers.put("NODE1", new InetSocketAddress(localHost, 3001));
		peers.put("NODE2", new InetSocketAddress(localHost, 3002));
		peers.put("NODE3", new InetSocketAddress(localHost, 3003));
		peers.put("NODE4", new InetSocketAddress(localHost, 3004));
		peers.put("NODE5", new InetSocketAddress(localHost, 3005));
		peers.put("NODE6", new InetSocketAddress(localHost, 3006));
		peers.put("NODE7", new InetSocketAddress(localHost, 3007));
		peers.put("NODE8", new InetSocketAddress(localHost, 3008));
		peers.put("NODE9", new InetSocketAddress(localHost, 3009));

		Map<String, ClusterProbe> probes = Util.createHashMap();
		for (String name : peers.keySet()) {
			ClusterProbe probe = new ClusterProbe(name, peers);
			probes.put(name, probe);
			probe.spawn();
		}

		Util.sleep(10 * 1000);

		for (Entry<String, ClusterProbe> e : probes.entrySet())
			System.out.println("HOST " + e.getKey() //
					+ ":" + e.getValue().dumpActivePeers());

		for (ClusterProbe probe : probes.values())
			probe.unspawn();

		for (ClusterProbe probe : probes.values())
			System.out.println(probe.dumpActivePeers());
	}

}
