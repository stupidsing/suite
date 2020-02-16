package suite.net.cluster;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static primal.statics.Rethrow.ex;
import static suite.util.Streamlet_.forInt;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.time.LocalDateTime;
import java.util.Map;

import org.junit.jupiter.api.Test;

import primal.MoreVerbs.Read;
import primal.Verbs.Sleep;
import primal.statics.Rethrow;
import suite.net.cluster.impl.ClusterProbeImpl;

public class ClusterProbeTest {

	private InetAddress localHost = Rethrow.ex(InetAddress::getLocalHost);

	@Test
	public void test() throws IOException {
		var nNodes = 3;

		var peers = forInt(nNodes).map2(i -> "NODE" + i, i -> new InetSocketAddress(localHost, 3000 + i)).toMap();

		var probes = Read //
				.from2(peers) //
				.keys() //
				.<String, ClusterProbe> map2(name -> name, name -> ex(() -> new ClusterProbeImpl(name, peers))) //
				.toMap();

		for (var probe : probes.values())
			probe.start();

		Sleep.quietly(10 * 1000);

		System.out.println("=== CLUSTER FORMED (" + LocalDateTime.now() + ") ===\n");
		dumpActivePeers(probes);
		assertActiveNodesSize(nNodes, probes);

		for (var probe : probes.values())
			probe.stop();

		Sleep.quietly(5 * 1000);

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
