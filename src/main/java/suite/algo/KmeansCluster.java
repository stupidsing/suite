package suite.algo;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import suite.math.linalg.Vector_;
import suite.primitive.Int_Int;
import suite.primitive.Ints_;
import suite.primitive.adt.map.IntObjMap;
import suite.primitive.adt.map.ObjIntMap;
import suite.primitive.adt.pair.IntFltPair;
import suite.streamlet.As;
import suite.streamlet.Read;
import suite.util.List_;
import suite.util.To;

public class KmeansCluster {

	private Vector_ vec = new Vector_();

	private int dimension;

	private class KmeansBin {
		private float[] sum = new float[dimension];
		private int count;
	}

	public KmeansCluster(int dimension) {
		this.dimension = dimension;
	}

	public <K> String result(Map<K, float[]> points, int k, int nIterations) {
		return kMeansCluster(points, k, nIterations) //
				.streamlet() //
				.groupBy() //
				.map((symbol, groups) -> Read.from(groups).map(Object::toString).collect(As.joinedBy(",")) + "\n") //
				.collect(As::joined);
	}

	public <K> ObjIntMap<K> kMeansCluster(Map<K, float[]> points, int k, int nIterations) {
		List<K> keys = new ArrayList<>(points.keySet());
		List<float[]> values = Read.from(keys).map(points::get).toList();
		int[] ks = kMeansCluster(values, k, nIterations);
		ObjIntMap<K> map = new ObjIntMap<>();

		for (int i : Ints_.range(ks.length))
			map.put(keys.get(i), ks[i]);

		return map;
	}

	public int[] kMeansCluster(List<float[]> points, int k, int nIterations) {
		List<float[]> centers = List_.left(points, k);
		int iteration = 0;

		while (true) {
			KmeansBin[] bins = To.array(KmeansBin.class, k, j -> new KmeansBin());

			for (float[] point : points) {
				KmeansBin bin = bins[findNearest(point, centers)];
				bin.sum = vec.add(point, bin.sum);
				bin.count++;
			}

			if (iteration++ <= nIterations)
				centers = Read.from(bins).map(bin -> div(bin.sum, bin.count)).toList();
			else {
				List<float[]> kMeans0 = centers;
				return Ints_.range(points.size()).collect(Int_Int.lift(i -> findNearest(points.get(i), kMeans0))).toArray();
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

		return map.streamlet().min((k, v) -> -v.t1.get()).t0;
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
		float[] d = vec.sub(a, b);
		return vec.dot(d);
	}

	private float[] div(float[] a, double b) {
		return vec.scale(a, 1d / b);
	}

}
