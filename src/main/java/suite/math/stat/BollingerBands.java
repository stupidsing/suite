package suite.math.stat;

import java.util.Arrays;

import suite.math.stat.Statistic.MeanVariance;

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
		public final float[] percentbs;
		public final float[] bandwidths;

		private Bb(float[] fs, int backPos0, int backPos1, float k) {
			int length = fs.length;
			lowers = new float[length];
			uppers = new float[length];
			percentbs = new float[length];
			bandwidths = new float[length];

			for (int i = 0; i < length; i++) {
				int i1 = i + 1;
				int s = Math.max(0, i1 - backPos0);
				int e = Math.max(0, i1 - backPos1);
				MeanVariance mv = stat.meanVariance(Arrays.copyOfRange(fs, s, e));
				double ksd = k * mv.standardDeviation();
				double bbl = mv.mean - ksd;
				double bbu = mv.mean + ksd;
				double diff = bbu - bbl;
				lowers[i] = (float) bbl;
				uppers[i] = (float) bbu;
				percentbs[i] = (float) ((fs[i] - bbl) / diff);
				bandwidths[i] = (float) (diff / mv.mean);
			}
		}
	}

}
