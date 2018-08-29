package suite.net.cluster;

import static org.junit.Assert.assertEquals;
import static suite.util.Friends.rethrow;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Random;

import org.junit.Test;

import suite.net.cluster.impl.ClusterImpl;
import suite.net.cluster.impl.ClusterMapImpl;
import suite.primitive.Ints_;
import suite.streamlet.Read;
import suite.util.Rethrow;
import suite.util.Thread_;

public class ClusterMapTest {

	private static Random random = new Random();

	private InetAddress localHost = Rethrow.ex(() -> InetAddress.getLocalHost());

	@Test
	public void testClusterMap() throws IOException {
		var nNodes = 3;

		var peers = Ints_.range(nNodes).map2(i -> "NODE" + i, i -> new InetSocketAddress(localHost, 3000 + i)).toMap();

		var clusters = Read //
				.from2(peers) //
				.keys() //
				.<String, Cluster> map2(name -> name, name -> rethrow(() -> new ClusterImpl(name, peers))) //
				.toMap();

		for (var cluster : clusters.values())
			cluster.start();

		var peerNames = new ArrayList<String>(peers.keySet());
		var clMap = Read.from2(peers).keys().map2(name -> name, name -> new ClusterMapImpl<>(clusters.get(name))).toMap();

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
