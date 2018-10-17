package suite.algo;

import static java.util.Map.entry;
import static org.junit.Assert.assertEquals;
import static suite.util.Friends.forInt;

import java.util.Map;
import java.util.Random;

import org.junit.Test;

import suite.math.linalg.Vector;
import suite.streamlet.FunUtil.Source;
import suite.streamlet.Read;

public class KMeansClusterTest {

	private Random random = new Random();
	private Vector vec = new Vector();

	@Test
	public void test() {
		var n = 3;

		var seeds = Map.<String, Source<float[]>> ofEntries( //
				entry("A", () -> point(-16f, 16f, 16f)), //
				entry("B", () -> point(16f, -16f, 16f)), //
				entry("C", () -> point(16f, 16f, -16f)));

		var points = Read //
				.from2(seeds) //
				.concatMap2((prefix, source) -> forInt(n).map2(i -> prefix + i, i -> source.g())) //
				.toMap();

		var kmc = new KmeansCluster(seeds.size());
		var clusters = kmc.kMeansCluster(points, n, 9);

		assertEquals(9, clusters.size());

		for (var prefix : seeds.keySet())
			for (var i : forInt(n))
				assertEquals(clusters.get(prefix + "0"), clusters.get(prefix + i));
	}

	private float[] point(float x, float y, float z) {
		return vec.of( //
				x + random.nextGaussian(), //
				y + random.nextGaussian(), //
				z + random.nextGaussian());
	}

}
