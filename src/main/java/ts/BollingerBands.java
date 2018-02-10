package ts;

import java.util.Arrays;

import suite.adt.pair.Pair;
import suite.math.linalg.VirtualVector;
import suite.math.numeric.Statistic;
import suite.math.numeric.Statistic.MeanVariance;
import suite.primitive.Int_Flt;

public class BollingerBands {

	private Statistic stat = new Statistic();

	public Bb bb(float[] fs, int backPos0, int backPos1, float k) {
		return new Bb(fs, backPos0, backPos1, k);
	}

	public Pair<float[], float[]> meanVariances(VirtualVector v, int backPos0, int backPos1) {
		int length = v.length;
		Int_Flt fun = v.get;

		float[] means = new float[length];
		float[] variances = new float[length];
		int d = backPos0 - backPos1;
		double il = 1d / d;
		int i = 0, j;
		double sum = 0d;
		double sumSq = 0d;

		for (; i < d; i++) {
			float f = fun.apply(i);
			sum += f;
			sumSq += f * f;
		}

		for (; (j = i + backPos1) < length; i++) {
			double mean = sum * il;
			means[j] = (float) mean;
			variances[j] = (float) (sumSq * il - mean * mean);

			float f0 = fun.apply(i - d);
			float fx = fun.apply(i);
			sum += fx - f0;
			sumSq += fx * fx - f0 * f0;
		}

		return Pair.of(means, variances);
	}

	public class Bb {
		public final float[] lowers;
		public final float[] uppers;
		public final float[] sds;
		public final float[] bandwidths;

		private Bb(float[] fs, int backPos0, int backPos1, float k) {
			int length = fs.length;
			lowers = new float[length];
			uppers = new float[length];
			sds = new float[length];
			bandwidths = new float[length];

			for (int i = 0; i < length; i++) {
				int i1 = i + 1;
				int s = Math.max(0, i1 - backPos0);
				int e = Math.max(0, i1 - backPos1);
				MeanVariance mv = stat.meanVariance(Arrays.copyOfRange(fs, s, e));
				double mean = mv.mean;
				double ksd = k * mv.standardDeviation();
				double bbl = mean - ksd;
				double bbu = mean + ksd;
				double diff = bbu - bbl;
				lowers[i] = (float) bbl;
				uppers[i] = (float) bbu;
				sds[i] = (float) ((fs[i] - mean) / diff);
				bandwidths[i] = (float) (diff / mean);
			}
		}
	}

}
