package suite.algo;

import static org.junit.Assert.assertEquals;

import java.util.Map;
import java.util.Random;

import org.junit.Test;

import suite.primitive.Ints_;
import suite.primitive.adt.map.ObjIntMap;
import suite.streamlet.Read;
import suite.util.FunUtil.Source;

public class KMeansClusterTest {

	private Random random = new Random();

	@Test
	public void test() {
		int n = 3;

		Map<String, Source<float[]>> seeds = Read //
				.<String, Source<float[]>> empty2() //
				.cons("A", () -> point(-16f, 16f, 16f)) //
				.cons("B", () -> point(16f, -16f, 16f)) //
				.cons("C", () -> point(16f, 16f, -16f)) //
				.toMap();

		Map<String, float[]> points = Read //
				.from2(seeds) //
				.concatMap2((prefix, source) -> Ints_ //
						.range(n) //
						.map2(i -> prefix + i, i -> source.source())) //
				.toMap();

		KmeansCluster kmc = new KmeansCluster(seeds.size());
		ObjIntMap<String> clusters = kmc.kmeansCluster(points, n, 9);

		assertEquals(9, clusters.size());

		for (String prefix : seeds.keySet())
			for (int i : Ints_.range(n))
				assertEquals(clusters.get(prefix + "0"), clusters.get(prefix + i));
	}

	private float[] point(float x, float y, float z) {
		return new float[] { //
				(float) (x + random.nextGaussian()), //
				(float) (y + random.nextGaussian()), //
				(float) (z + random.nextGaussian()), //
		};
	}

}
