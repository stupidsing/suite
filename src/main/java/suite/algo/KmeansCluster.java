package suite.algo;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import suite.math.Matrix;
import suite.streamlet.Read;
import suite.util.To;
import suite.util.Util;

public class KmeansCluster {

	private static Matrix mtx = new Matrix();

	private int dimension;

	private class KmeansBin {
		private float[] sum = new float[dimension];
		private int count;
	}

	private class KnnBin {
		private int category;
		private float sqdist;
	}

	public KmeansCluster(int dimension) {
		this.dimension = dimension;
	}

	public Collection<List<float[]>> kmeansCluster(List<float[]> points, int k, int nIterations) {
		List<float[]> kmeans = Util.left(points, k);
		int iteration = 0;

		while (true) {
			KmeansBin bins[] = To.array(KmeansBin.class, k, j -> new KmeansBin());

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
		List<KnnBin> bins = Read.from(points) //
				.index() //
				.map((i, point) -> {
					KnnBin bin = new KnnBin();
					bin.category = i;
					bin.sqdist = sqdist(point0, point);
					return bin;
				}) //
				.toList();

		Map<Integer, AtomicInteger> map = new HashMap<>();

		Read.from(bins) //
				.sort((b0, b1) -> Float.compare(b0.sqdist, b1.sqdist)) //
				.take(points.size()) //
				.forEach(bin -> map.computeIfAbsent(bin.category, c -> new AtomicInteger()).incrementAndGet());

		return Read.from2(map).min((k, v) -> -v.t1.get()).t0;
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
		return mtx.dot(d, d);
	}

	private float[] div(float[] a, float b) {
		return mtx.scale(a, 1f / b);
	}

}
