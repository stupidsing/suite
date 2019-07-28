package suite.net.cluster;

import static org.junit.Assert.assertEquals;
import static suite.util.Friends.rethrow;
import static suite.util.Streamlet_.forInt;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.time.LocalDateTime;
import java.util.Map;

import org.junit.Test;

import suite.net.cluster.impl.ClusterProbeImpl;
import suite.streamlet.Read;
import suite.util.Rethrow;
import suite.util.Thread_;

public class ClusterProbeTest {

	private InetAddress localHost = Rethrow.ex(InetAddress::getLocalHost);

	@Test
	public void test() throws IOException {
		var nNodes = 3;

		var peers = forInt(nNodes).map2(i -> "NODE" + i, i -> new InetSocketAddress(localHost, 3000 + i)).toMap();

		var probes = Read //
				.from2(peers) //
				.keys() //
				.<String, ClusterProbe> map2(name -> name, name -> rethrow(() -> new ClusterProbeImpl(name, peers))) //
				.toMap();

		for (var probe : probes.values())
			probe.start();

		Thread_.sleepQuietly(10 * 1000);

		System.out.println("=== CLUSTER FORMED (" + LocalDateTime.now() + ") ===\n");
		dumpActivePeers(probes);
		assertActiveNodesSize(nNodes, probes);

		for (var probe : probes.values())
			probe.stop();

		Thread_.sleepQuietly(5 * 1000);

		System.out.println("=== CLUSTER STOPPED (" + LocalDateTime.now() + ") ===\n");
		dumpActivePeers(probes);
		assertActiveNodesSize(0, probes);
	}

	private void dumpActivePeers(Map<String, ClusterProbe> probes) {
		for (var e : probes.entrySet()) {
			System.out.println("HOST " + e.getKey() + " -");
			System.out.println(e.getValue());
		}
	}

	private void assertActiveNodesSize(int nNodes, Map<String, ClusterProbe> probes) {
		for (var probe : probes.values())
			assertEquals(nNodes, probe.getActivePeers().size());
	}

}
