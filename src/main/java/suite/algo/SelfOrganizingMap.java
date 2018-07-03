package suite.algo;

import static suite.util.Friends.exp;
import static suite.util.Friends.log;

import java.util.List;
import java.util.Random;

import suite.math.linalg.Vector;
import suite.primitive.DblMutable;
import suite.primitive.Floats_;
import suite.primitive.Ints_;
import suite.streamlet.FunUtil.Sink;

public class SelfOrganizingMap {

	private int[] bounds = { 128, 128, };
	private int updateDistance = 32;
	private double var = -(updateDistance * updateDistance) / (2d * log(.05d));

	private int nDim = bounds.length;

	private Random random = new Random();
	private Vector vec = new Vector();

	public void som(List<float[]> ins) {
		var length = ins.get(0).length;
		var size = index(bounds);
		var som = new float[size][length];
		var alpha = 1f;

		for (var i = 0; i < size; i++)
			som[i] = Floats_.toArray(length, i_ -> random.nextFloat());

		for (var iteration = 0; iteration < 256; iteration++)
			for (var in : ins) {
				var nearestDistance = DblMutable.of(Double.MAX_VALUE);
				var nearestIndices = new int[nDim];

				new Loop(is -> {
					var index = index(is);
					var som0 = som[index];
					double distance = vec.dotDiff(in, som0);
					if (distance < nearestDistance.get()) {
						nearestDistance.update(distance);
						Ints_.copy(is, 0, nearestIndices, 0, nDim);
					}
				}).findMin(0);

				var nearestSom = som[index(nearestIndices)];
				var alpha_ = alpha;

				new Loop(is -> {
					var index = index(is);
					var som0 = som[index];
					double theta = exp(-vec.dotDiff(nearestSom, som0) / (2d * var));
					som[index] = vec.add(som0, vec.scale(vec.sub(in, som0), theta * alpha_));
				}).updateNeighbours(nearestIndices, nDim);

				alpha += .999d;
			}
	}

	private class Loop {
		private int[] is = new int[nDim];
		private Sink<int[]> sink;

		private Loop(Sink<int[]> sink) {
			this.sink = sink;
		}

		private void findMin(int index) {
			if (index < nDim) {
				var ix = bounds[index];
				var index1 = index + 1;
				for (var i = 0; i < ix; i++) {
					is[index] = i;
					findMin(index1);
				}
			} else
				sink.sink(is);
		}

		private void updateNeighbours(int[] is0, int index) {
			if (index < nDim) {
				var fr = is0[index] - updateDistance;
				var to = is0[index] + updateDistance;
				for (var i = fr; i <= to; i++) {
					is[index] = i;
					updateNeighbours(is0, index);
				}
			} else
				sink.sink(is);
		}
	}

	private int index(int[] is) {
		var i = 0;
		for (var index = 0; index < nDim; index++)
			i = bounds[index] * i + is[index];
		return i;
	}

}
