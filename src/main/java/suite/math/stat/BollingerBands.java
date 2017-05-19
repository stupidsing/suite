package suite.math.stat;

import java.util.Arrays;

import suite.math.stat.Statistic.MeanVariance;
import suite.trade.MovingAverage;

/**
 * Auto-regressive distributed lag.
 *
 * @author ywsing
 */
public class BollingerBands {

	private MovingAverage ma = new MovingAverage();
	private Statistic stat = new Statistic();

	public Bb bb(float[] fs, int n, int k) {
		return new Bb(fs, n, k);
	}

	public class Bb {
		public final float[] lower;
		public final float[] upper;
		public final float[] percentb;
		public final float[] bandwidth;

		private Bb(float[] fs, int n, int k) {
			float[] movingAvg = ma.movingAvg(fs, n);
			int length = movingAvg.length;
			lower = new float[length];
			upper = new float[length];
			percentb = new float[length];
			bandwidth = new float[length];

			for (int i = n; i < length; i++) {
				MeanVariance mv = stat.meanVariance(Arrays.copyOfRange(fs, i - n, i));
				double ksd = k * mv.standardDeviation();
				double bbl = mv.mean - ksd;
				double bbu = mv.mean + ksd;
				lower[i] = (float) bbl;
				upper[i] = (float) bbu;
				percentb[i] = (float) ((fs[i] - bbl) / (bbu - bbl));
				bandwidth[i] = (float) ((bbu - bbl) / mv.mean);
			}
		}
	}

}
