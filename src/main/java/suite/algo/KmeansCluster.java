package suite.algo;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import primal.MoreVerbs.Read;
import primal.Verbs.Left;
import primal.Verbs.New;
import primal.primitive.IntMoreVerbs.LiftInt;
import primal.primitive.IntMoreVerbs.ReadInt;
import primal.primitive.IntPrim.Obj_Int;
import primal.primitive.adt.map.IntObjMap;
import primal.primitive.adt.map.ObjIntMap;
import primal.primitive.adt.pair.DblObjPair;
import primal.primitive.adt.pair.IntDblPair;
import suite.math.linalg.Vector;

public class KmeansCluster {

	private Vector vec = new Vector();

	private int dimension;

	private class KmeansBin {
		private float[] sum = new float[dimension];
		private int count;
	}

	public KmeansCluster(int dimension) {
		this.dimension = dimension;
	}

	public <K> String result(Map<K, float[]> points, int k, int nIterations) {
		return ReadInt //
				.from2(kMeansCluster(points, k, nIterations)) //
				.groupBy() //
				.map((symbol, groups) -> Read.from(groups).map(Object::toString).toJoinedString(",")) //
				.toLines();
	}

	public <K> ObjIntMap<K> kMeansCluster(Map<K, float[]> points, int k, int nIterations) {
		var keys = new ArrayList<>(points.keySet());
		var values = Read.from(keys).map(points::get).toList();
		var classifier = kMeansClusterClassifier(values, k, nIterations);
		var map = new ObjIntMap<K>();
		for (var e : points.entrySet())
			map.put(e.getKey(), classifier.apply(e.getValue()));
		return map;
	}

	public int[] kMeansCluster(List<float[]> points, int k, int nIterations) {
		return Read.from(points).collect(LiftInt.of(kMeansClusterClassifier(points, k, nIterations))).toArray();
	}

	private Obj_Int<float[]> kMeansClusterClassifier(List<float[]> points, int k, int nIterations) {
		var centers = Left.of(points, k);

		for (var iteration = 0; iteration < nIterations; iteration++) {
			var bins = New.array(k, KmeansBin.class, j -> new KmeansBin());

			for (var point : points) {
				var bin = bins[findNearest(point, centers)];
				bin.sum = vec.add(point, bin.sum);
				bin.count++;
			}

			centers = Read.from(bins).map(bin -> div(bin.sum, bin.count)).toList();
		}

		var centers_ = centers;
		return point -> findNearest(point, centers_);
	}

	public int kNearestNeighbor(List<float[]> points, float[] point0) {
		var map = new IntObjMap<AtomicInteger>();

		Read //
				.from(points) //
				.index() //
				.map((i, point) -> IntDblPair.of(i, sqdist(point0, point))) //
				.sortBy(pair -> pair.t1) //
				.take(points.size()) //
				.forEach(bin -> map.computeIfAbsent(bin.t0, c -> new AtomicInteger()).incrementAndGet());

		return ReadInt.from2(map).min((k, v) -> -v.v.get()).k;
	}

	private int findNearest(float[] point, List<float[]> points) {
		var min = DblObjPair.of(Double.MAX_VALUE, 0);
		for (var j = 0; j < points.size(); j++) {
			var dist = sqdist(point, points.get(j));
			if (dist < min.k)
				min.update(dist, j);
		}
		return min.v;
	}

	private double sqdist(float[] a, float[] b) {
		var d = vec.sub(a, b);
		return vec.dot(d);
	}

	private float[] div(float[] a, double b) {
		return vec.scale(a, 1d / b);
	}

}
