package suite.ts;

import primal.adt.Pair;
import suite.math.linalg.VirtualVector;
import suite.math.numeric.Statistic;

import java.util.Arrays;

import static java.lang.Math.max;

public class BollingerBands {

	private Statistic stat = new Statistic();

	public Bb bb(float[] fs, int backPos0, int backPos1, float k) {
		return new Bb(fs, backPos0, backPos1, k);
	}

	public Pair<float[], float[]> meanVariances(VirtualVector v, int backPos0, int backPos1) {
		var length = v.length;
		var fun = v.get;

		var means = new float[length];
		var variances = new float[length];
		var d = backPos0 - backPos1;
		var il = 1d / d;
		int i = 0, j;
		var sum = 0d;
		var sumSq = 0d;

		for (; i < d; i++) {
			var f = fun.apply(i);
			sum += f;
			sumSq += f * f;
		}

		for (; (j = i + backPos1) < length; i++) {
			var mean = sum * il;
			means[j] = (float) mean;
			variances[j] = (float) (sumSq * il - mean * mean);

			var f0 = fun.apply(i - d);
			var fx = fun.apply(i);
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
			var length = fs.length;
			lowers = new float[length];
			uppers = new float[length];
			sds = new float[length];
			bandwidths = new float[length];

			for (var i = 0; i < length; i++) {
				var i1 = i + 1;
				var s = max(0, i1 - backPos0);
				var e = max(0, i1 - backPos1);
				var mv = stat.meanVariance(Arrays.copyOfRange(fs, s, e));
				var mean = mv.mean;
				var ksd = k * mv.standardDeviation();
				var bbl = mean - ksd;
				var bbu = mean + ksd;
				var diff = bbu - bbl;
				lowers[i] = (float) bbl;
				uppers[i] = (float) bbu;
				sds[i] = (float) ((fs[i] - mean) / diff);
				bandwidths[i] = (float) (diff / mean);
			}
		}
	}

}
