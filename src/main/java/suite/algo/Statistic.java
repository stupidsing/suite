package suite.algo;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import suite.streamlet.Read;
import suite.util.Util;

public class Statistic {

	private int dimension;

	private class KmeansBin {
		private float sum[] = new float[dimension];
		private int count;
	}

	private class KnnBin {
		private int category;
		private float sqdist;
	}

	public Statistic(int dimension) {
		this.dimension = dimension;
	}

	public Collection<List<float[]>> kmeansCluster(List<float[]> points, int k, int nIterations) {
		List<float[]> kmeans = Util.left(points, k);
		int iteration = 0;

		while (true) {
			KmeansBin bins[] = new KmeansBin[k];

			for (int j = 0; j < k; j++)
				bins[j] = new KmeansBin();

			for (float point[] : points) {
				KmeansBin bin = bins[findNearest(point, kmeans)];
				bin.sum = add(point, bin.sum);
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

	public int kNearestNeighbor(List<float[]> points, float point0[]) {
		List<KnnBin> bins = new ArrayList<>();

		for (int i = 0; i < points.size(); i++) {
			KnnBin bin = new KnnBin();
			bin.category = i;
			bin.sqdist = sqdist(point0, points.get(i));
		}

		Map<Integer, AtomicInteger> map = new HashMap<>();

		Read.from(bins) //
				.sort((b0, b1) -> Float.compare(b0.sqdist, b1.sqdist)) //
				.take(points.size()) //
				.forEach(bin -> map.computeIfAbsent(bin.category, c -> new AtomicInteger()).incrementAndGet());

		return Read.from2(map).min((k, v) -> -v.t1.get()).t0;
	}

	private int findNearest(float point[], List<float[]> points) {
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

	private float sqdist(float a[], float b[]) {
		float d[] = sub(a, b);
		return dot(d, d);
	}

	private float[] add(float a[], float b[]) {
		float fs[] = new float[dimension];
		for (int i = 0; i < dimension; i++)
			fs[i] = a[i] + b[i];
		return fs;
	}

	private float[] sub(float a[], float b[]) {
		float fs[] = new float[dimension];
		for (int i = 0; i < dimension; i++)
			fs[i] = a[i] - b[i];
		return fs;
	}

	private float[] div(float a[], float b) {
		float fs[] = new float[dimension];
		for (int i = 0; i < dimension; i++)
			fs[i] = a[i] / b;
		return fs;
	}

	private float dot(float a[], float b[]) {
		float dot = 0f;
		for (int i = 0; i < dimension; i++)
			dot += a[i] * b[i];
		return dot;
	}

}
