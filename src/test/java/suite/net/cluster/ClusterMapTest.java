package suite.net.cluster;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.junit.Test;

import suite.util.Util;

public class ClusterMapTest {

	private static Random random = new Random();

	@Test
	public void testClusterMap() throws IOException {
		int nNodes = 3;
		InetAddress localHost = InetAddress.getLocalHost();

		Map<String, InetSocketAddress> peers = new HashMap<>();
		for (int i = 0; i < nNodes; i++)
			peers.put("NODE" + i, new InetSocketAddress(localHost, 3000 + i));

		List<String> peerNames = new ArrayList<>(peers.keySet());
		Map<String, Cluster> clusters = new HashMap<>();
		Map<String, ClusterMap<Integer, String>> clMap = new HashMap<>();

		for (String name : peers.keySet()) {
			Cluster cluster = new Cluster(name, peers);
			clusters.put(name, cluster);
			cluster.start();

			clMap.put(name, new ClusterMap<Integer, String>(cluster));
		}

		Util.sleepQuietly(5 * 1000);

		System.out.println("=== CLUSTER FORMED (" + new Date() + ") ===\n");

		for (int i = 0; i < 100; i++) {
			String peer = peerNames.get(random.nextInt(nNodes));
			clMap.get(peer).set(i, Integer.toString(i));
		}

		for (int i = 0; i < 100; i++) {
			String peer = peerNames.get(random.nextInt(nNodes));
			assertEquals(Integer.toString(i), clMap.get(peer).get(i));
		}

		for (Cluster cluster : clusters.values())
			cluster.stop();

		System.out.println("=== CLUSTER STOPPED (" + new Date() + ") ===\n");
	}

}
