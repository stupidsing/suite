package suite.net.cluster;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Random;

import org.junit.Test;

import suite.net.cluster.impl.ClusterImpl;
import suite.net.cluster.impl.ClusterMapImpl;
import suite.util.Rethrow;
import suite.util.Thread_;

public class ClusterMapTest {

	private static Random random = new Random();

	private InetAddress localHost = Rethrow.ex(() -> InetAddress.getLocalHost());

	@Test
	public void testClusterMap() throws IOException {
		var nNodes = 3;
		var peers = new HashMap<String, InetSocketAddress>();

		for (var i = 0; i < nNodes; i++)
			peers.put("NODE" + i, new InetSocketAddress(localHost, 3000 + i));

		var peerNames = new ArrayList<String>(peers.keySet());
		var clusters = new HashMap<String, Cluster>();
		var clMap = new HashMap<String, ClusterMap<Integer, String>>();

		for (var name : peers.keySet()) {
			var cluster = new ClusterImpl(name, peers);
			clusters.put(name, cluster);
			cluster.start();

			clMap.put(name, new ClusterMapImpl<>(cluster));
		}

		Thread_.sleepQuietly(5 * 1000);

		System.out.println("=== CLUSTER FORMED (" + LocalDateTime.now() + ") ===\n");

		for (var i = 0; i < 100; i++) {
			var peer = peerNames.get(random.nextInt(nNodes));
			clMap.get(peer).set(i, Integer.toString(i));
		}

		for (var i = 0; i < 100; i++) {
			var peer = peerNames.get(random.nextInt(nNodes));
			assertEquals(Integer.toString(i), clMap.get(peer).get(i));
		}

		for (var cluster : clusters.values())
			cluster.stop();

		System.out.println("=== CLUSTER STOPPED (" + LocalDateTime.now() + ") ===\n");
	}

}
