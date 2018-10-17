package suite.net.cluster;

import static org.junit.Assert.assertEquals;
import static suite.util.Friends.fail;
import static suite.util.Friends.forInt;
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
import suite.streamlet.FunUtil.Fun;
import suite.streamlet.FunUtil.Sink;
import suite.streamlet.FunUtil.Source;
import suite.streamlet.Read;
import suite.streamlet.Streamlet;
import suite.util.Rethrow;
import suite.util.Thread_;

public class NioClusterMapTest {

	private static Random random = new Random();

	private InetAddress localHost = Rethrow.ex(InetAddress::getLocalHost);
	private Sink<IOException> fail = LogUtil::error;

	@Test
	public void testClusterMap() throws IOException {
		var nNodes = 3;

		var peers = forInt(nNodes).map2(i -> "NODE" + i, i -> new InetSocketAddress(localHost, 3000 + i)).toMap();

		var clusters = Read //
				.from2(peers) //
				.keys() //
				.map2(name -> name, name -> rethrow(() -> new NioCluster(name, peers))) //
				.toMap();

		for (var cluster : clusters.values())
			cluster.start();

		var peerNames = new ArrayList<>(peers.keySet());

		var clMap = Read //
				.from2(peers) //
				.keys() //
				.map2(name -> name, name -> new NioClusterMap<Integer, String>(clusters.get(name))) //
				.toMap();

		Thread_.sleepQuietly(5 * 1000);

		System.out.println("=== CLUSTER FORMED (" + LocalDateTime.now() + ") ===\n");

		Source<NioClusterMap<Integer, String>> peerf = () -> clMap.get(peerNames.get(random.nextInt(nNodes)));

		Int_Obj<Sink<Runnable>> setf = i -> cont -> peerf.g().set(i, Integer.toString(i), v0 -> cont.run(), fail);

		Int_Obj<Sink<Runnable>> getf = i -> cont -> peerf.g().get(i, v -> {
			assertEquals(Integer.toString(i), v);
			cont.run();
		}, fail);

		Fun<NioCluster, Sink<Runnable>> closef = cluster -> cont -> {
			try {
				cluster.stop();
				System.out.println("=== CLUSTER STOPPED (" + LocalDateTime.now() + ") ===\n");
			} catch (IOException ex) {
				fail(ex);
			}
			cont.run();
		};

		var sinks = Streamlet.concat( //
				forInt(9).map(setf), //
				forInt(9).map(getf), //
				Read.from2(clusters).values().map(closef)).toList();

		new Object() {
			public void run(int i) {
				if (i < sinks.size())
					sinks.get(i).f(() -> run(i + 1));
			}
		}.run(0);

		Read.from2(clusters).values().map(cluster -> Thread_.newThread(cluster::run)).collect(Thread_::startJoin);

		for (var cluster : clusters.values())
			cluster.close();
	}

}
