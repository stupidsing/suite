package suite.math.stat;

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
	private TimeSeries ts = new TimeSeries();

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

			for (int i = 0; i < length; i++) {
				MeanVariance mv = stat.meanVariance(ts.back(i - 1, n, fs));
				double ksd = k * mv.standardDeviation();
				double bbl = mv.mean - ksd;
				double bbu = mv.mean + ksd;
				double diff = bbu - bbl;
				lower[i] = (float) bbl;
				upper[i] = (float) bbu;
				percentb[i] = (float) ((fs[i] - bbl) / diff);
				bandwidth[i] = (float) (diff / mv.mean);
			}
		}
	}

}
