package suite.algo;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import suite.math.linalg.Matrix;
import suite.primitive.adt.map.IntObjMap;
import suite.primitive.adt.pair.IntFltPair;
import suite.streamlet.Read;
import suite.util.List_;
import suite.util.To;

public class KmeansCluster {

	private Matrix mtx = new Matrix();

	private int dimension;

	private class KmeansBin {
		private float[] sum = new float[dimension];
		private int count;
	}

	public KmeansCluster(int dimension) {
		this.dimension = dimension;
	}

	public Collection<List<float[]>> kmeansCluster(List<float[]> points, int k, int nIterations) {
		List<float[]> kmeans = List_.left(points, k);
		int iteration = 0;

		while (true) {
			KmeansBin[] bins = To.array(KmeansBin.class, k, j -> new KmeansBin());

			for (float[] point : points) {
				KmeansBin bin = bins[findNearest(point, kmeans)];
				bin.sum = mtx.add(point, bin.sum);
				bin.count++;
			}

			if (iteration++ <= nIterations)
				kmeans = Read.from(bins).map(bin -> div(bin.sum, bin.count)).toList();
			else {
				List<float[]> kmeans0 = kmeans;
				return Read.from(points).toListMap(point -> findNearest(point, kmeans0)).values();
			}
		}
	}

	public int kNearestNeighbor(List<float[]> points, float[] point0) {
		IntObjMap<AtomicInteger> map = new IntObjMap<>();

		Read //
				.from(points) //
				.index() //
				.map((i, point) -> IntFltPair.of(i, sqdist(point0, point))) //
				.sortBy(pair -> pair.t1) //
				.take(points.size()) //
				.forEach(bin -> map.computeIfAbsent(bin.t0, c -> new AtomicInteger()).incrementAndGet());

		return map.stream().min((k, v) -> -v.t1.get()).t0;
	}

	private int findNearest(float[] point, List<float[]> points) {
		float minDist = Float.MAX_VALUE;
		int minj = 0;
		for (int j = 0; j < points.size(); j++) {
			float dist = sqdist(point, points.get(j));
			if (dist < minDist) {
				minDist = dist;
				minj = j;
			}
		}
		return minj;
	}

	private float sqdist(float[] a, float[] b) {
		float[] d = mtx.sub(a, b);
		return mtx.dot(d);
	}

	private float[] div(float[] a, float b) {
		return mtx.scale(a, 1f / b);
	}

}
