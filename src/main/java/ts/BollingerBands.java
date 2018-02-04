package ts;

import java.util.Arrays;

import suite.math.numeric.Statistic;
import suite.math.numeric.Statistic.MeanVariance;

/**
 * Auto-regressive distributed lag.
 *
 * @author ywsing
 */
public class BollingerBands {

	private Statistic stat = new Statistic();

	public Bb bb(float[] fs, int backPos0, int backPos1, float k) {
		return new Bb(fs, backPos0, backPos1, k);
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
