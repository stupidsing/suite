package suite.net.cluster;

import static java.util.Map.entry;
import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.time.LocalDateTime;
import java.util.Map;

import org.junit.Test;

import suite.net.cluster.impl.ClusterImpl;
import suite.util.Rethrow;
import suite.util.Thread_;

public class ClusterTest {

	private InetAddress localHost = Rethrow.ex(() -> InetAddress.getLocalHost());

	@Test
	public void testCluster() throws IOException {
		var peers = Map.ofEntries( //
				entry("NODE0", new InetSocketAddress(localHost, 3000)), //
				entry("NODE1", new InetSocketAddress(localHost, 3001)));

		var cluster0 = new ClusterImpl("NODE0", peers);
		var cluster1 = new ClusterImpl("NODE1", peers);

		cluster1.setOnReceive(Integer.class, i -> i + 1);

		cluster0.start();
		cluster1.start();

		Thread_.sleepQuietly(2 * 1000);

		System.out.println("=== CLUSTER FORMED (" + LocalDateTime.now() + ") ===\n");

		assertEquals(12346, cluster0.requestForResponse("NODE1", 12345));

		cluster0.stop();
		cluster1.stop();

		System.out.println("=== CLUSTER STOPPED (" + LocalDateTime.now() + ") ===\n");
	}

}
