package suite.algo;

import java.util.List;
import java.util.Random;

import suite.math.linalg.Vector_;
import suite.primitive.Floats_;
import suite.primitive.FltMutable;
import suite.primitive.Ints_;
import suite.util.FunUtil.Sink;

public class SelfOrganizingMap {

	private int[] bounds = { 128, 128, };
	private int updateDistance = 32;
	private double var = -(updateDistance * updateDistance) / (2d * Math.log(.05d));

	private int nDim = bounds.length;

	private Random random = new Random();
	private Vector_ vec = new Vector_();

	public void som(List<float[]> ins) {
		int length = ins.get(0).length;
		int size = index(bounds);
		float[][] som = new float[size][length];
		double alpha = 1f;

		for (int i = 0; i < size; i++)
			som[i] = Floats_.toArray(length, i_ -> random.nextFloat());

		for (int iteration = 0; iteration < 256; iteration++)
			for (float[] in : ins) {
				FltMutable nearestDistance = FltMutable.of(Float.MAX_VALUE);
				int[] nearestIndices = new int[nDim];

				new Loop(is -> {
					int index = index(is);
					float[] som0 = som[index];
					float distance = vec.dotDiff(in, som0);
					if (distance < nearestDistance.get()) {
						nearestDistance.update(distance);
						Ints_.copy(is, 0, nearestIndices, 0, nDim);
					}
				}).findMin(0);

				float[] nearestSom = som[index(nearestIndices)];
				double alpha_ = alpha;

				new Loop(is -> {
					int index = index(is);
					float[] som0 = som[index];
					double theta = Math.exp(-vec.dotDiff(nearestSom, som0) / (2d * var));
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
				int ix = bounds[index];
				int index1 = index + 1;
				for (int i = 0; i < ix; i++) {
					is[index] = i;
					findMin(index1);
				}
			} else
				sink.sink(is);
		}

		private void updateNeighbours(int[] is0, int index) {
			if (index < nDim) {
				int fr = is0[index] - updateDistance;
				int to = is0[index] + updateDistance;
				for (int i = fr; i <= to; i++) {
					is[index] = i;
					updateNeighbours(is0, index);
				}
			} else
				sink.sink(is);
		}
	}

	private int index(int[] is) {
		int i = 0;
		for (int index = 0; index < nDim; index++)
			i = bounds[index] * i + is[index];
		return i;
	}

}
