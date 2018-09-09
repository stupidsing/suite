package suite.net.cluster;

import static org.junit.Assert.assertEquals;
import static suite.util.Friends.fail;
import static suite.util.Friends.rethrow;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Random;

import org.junit.Test;

import suite.net.cluster.impl.NioCluster;
import suite.net.cluster.impl.NioClusterMap;
import suite.os.LogUtil;
import suite.primitive.IntPrimitives.Int_Obj;
import suite.primitive.Ints_;
import suite.streamlet.FunUtil.Sink;
import suite.streamlet.Read;
import suite.streamlet.Streamlet;
import suite.util.Rethrow;
import suite.util.Th;
import suite.util.Thread_;

public class NioClusterMapTest {

	private static Random random = new Random();

	private InetAddress localHost = Rethrow.ex(() -> InetAddress.getLocalHost());
	private Sink<IOException> fail = LogUtil::error;

	@Test
	public void testClusterMap() throws IOException {
		var nNodes = 3;

		var peers = Ints_.range(nNodes).map2(i -> "NODE" + i, i -> new InetSocketAddress(localHost, 3000 + i)).toMap();

		var clusters = Read //
				.from2(peers) //
				.keys() //
				.<String, NioCluster> map2(name -> name, name -> rethrow(() -> new NioCluster(name, peers))) //
				.toMap();

		for (var cluster : clusters.values())
			cluster.start();

		var peerNames = new ArrayList<>(peers.keySet());
		var clMap = Read.from2(peers).keys().map2(name -> name, name -> new NioClusterMap<>(clusters.get(name))).toMap();

		Thread_.sleepQuietly(5 * 1000);

		System.out.println("=== CLUSTER FORMED (" + LocalDateTime.now() + ") ===\n");

		Int_Obj<Sink<Runnable>> setf = i -> cont -> {
			var peer = peerNames.get(random.nextInt(nNodes));
			clMap.get(peer).set(i, Integer.toString(i), v0 -> cont.run(), fail);
		};

		Int_Obj<Sink<Runnable>> getf = i -> cont -> {
			var peer = peerNames.get(random.nextInt(nNodes));
			clMap.get(peer).get(i, v -> {
				assertEquals(Integer.toString(i), v);
				cont.run();
			}, fail);
		};

		var sinks = Streamlet.concat( //
				Ints_.range(100).map(setf), //
				Ints_.range(100).map(getf), //
				Read.each(cont -> {
					for (var cluster : clusters.values())
						try {
							cluster.stop();
							System.out.println("=== CLUSTER STOPPED (" + LocalDateTime.now() + ") ===\n");
						} catch (IOException ex) {
							fail(ex);
						}
				})).toList();

		new Object() {
			public void run(int i) {
				if (i < sinks.size())
					sinks.get(i).sink(() -> run(i + 1));
			}
		}.run(0);

		Thread_.startJoin(Read.from2(clusters).values().map(cluster -> new Th(cluster::run)));

		for (var cluster : clusters.values())
			cluster.close();
	}

}
