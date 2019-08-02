package suite.net.cluster;

import static java.util.Map.entry;
import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.time.LocalDateTime;
import java.util.Map;

import org.junit.Test;

import primal.Verbs.Sleep;
import primal.Verbs.Start;
import primal.os.Log_;
import primal.statics.Rethrow;
import suite.net.cluster.impl.NioCluster;

public class NioClusterTest {

	private InetAddress localHost = Rethrow.ex(InetAddress::getLocalHost);

	@Test
	public void testCluster() throws IOException {
		var peers = Map.ofEntries( //
				entry("NODE0", new InetSocketAddress(localHost, 3000)), //
				entry("NODE1", new InetSocketAddress(localHost, 3001)));

		try ( //
				var cluster0 = new NioCluster("NODE0", peers); //
				var cluster1 = new NioCluster("NODE1", peers);) {
			cluster1.setOnReceive(Integer.class, i -> i + 1);

			cluster0.start();
			cluster1.start();

			Sleep.quietly(2 * 1000);

			System.out.println("=== CLUSTER FORMED (" + LocalDateTime.now() + ") ===\n");

			cluster0.requestForResponse("NODE1", 12345, response -> {
				assertEquals(12346, response);
				System.out.println("OK");

				try {
					cluster0.stop();
					cluster1.stop();
				} catch (IOException ex) {
					throw new RuntimeException(ex);
				}

				System.out.println("=== CLUSTER STOPPED (" + LocalDateTime.now() + ") ===\n");
			}, Log_::error);

			Start.thenJoin(cluster0::run, cluster1::run);
		}
	}

}
