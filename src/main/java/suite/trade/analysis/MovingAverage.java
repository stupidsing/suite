package suite.trade.analysis;

import static suite.util.Friends.max;

import java.util.Arrays;

import suite.math.linalg.Vector;
import suite.primitive.Doubles_;
import suite.util.To;
import ts.TimeSeries;

public class MovingAverage {

	private TimeSeries ts = new TimeSeries();
	private Vector vec = new Vector();

	// exponential moving average convergence/divergence
	public float[] emacd(float[] prices, double alpha0, double alpha1) {
		float[] emas0 = exponentialMovingAvg(prices, alpha0); // long-term
		float[] emas1 = exponentialMovingAvg(prices, alpha1); // short-term
		return vec.sub(emas1, emas0);
	}

	// moving average convergence/divergence
	public Macd macd(float[] prices, int nDays0, int nDays1, int nDays2) {
		float[] mas0 = movingAvg(prices, nDays0); // long-term
		float[] mas1 = movingAvg(prices, nDays1); // short-term
		float[] diffs = vec.sub(mas1, mas0);
		return new Macd(diffs, movingAvg(diffs, nDays2));
	}

	public class Macd {
		public final float[] macds;
		public final float[] movingAvgMacds;

		private Macd(float[] macds, float[] movingAvgMacds) {
			this.macds = macds;
			this.movingAvgMacds = movingAvgMacds;
		}
	}

	public float[] exponentialGeometricMovingAvg(float[] prices, int halfLife) {
		return exponentialGeometricMovingAvg(prices, Math.exp(Math.log(.5d) * halfLife));
	}

	public float[] exponentialGeometricMovingAvg(float[] prices, double alpha) {
		float[] logPrices = To.vector(prices, Math::log);
		float[] movingAvgs = exponentialMovingAvg(logPrices, alpha);
		return To.vector(movingAvgs, Math::exp);
	}

	public float[] exponentialMovingAvg(float[] prices, int halfLife) {
		return exponentialMovingAvg(prices, Math.exp(Math.log(.5d) * halfLife));
	}

	public float[] exponentialMovingAvg(float[] prices, double alpha) {
		var length = prices.length;
		float[] emas = new float[length];
		var ema = 0 < length ? prices[0] : 0d;
		for (int day = 0; day < length; day++)
			emas[day] = (float) (ema += alpha * (prices[day] - ema));
		return emas;
	}

	public float[] geometricMovingAvg(float[] prices, int windowSize) {
		float[] logPrices = To.vector(prices, Math::log);
		float[] movingAvgs = movingAvg(logPrices, windowSize);
		return To.vector(movingAvgs, Math::exp);
	}

	public float[] movingAvg(float[] prices, int windowSize) {
		var length = prices.length;
		float[] movingAvgs = new float[length];
		var div = 1d / windowSize;
		var movingSum = 0 < length ? prices[0] * windowSize : 0d;

		for (int day = 0; day < length; day++) {
			movingSum += prices[day] - prices[max(0, day - windowSize)];
			movingAvgs[day] = (float) (movingSum * div);
		}

		return movingAvgs;
	}

	public MovingRange[] movingRange(float[] prices, int windowSize) {
		return To.array(prices.length, MovingRange.class, i -> {
			float[] window = ts.back(i, windowSize, prices);
			Arrays.sort(window);
			var length = window.length;
			return new MovingRange(window[0], window[length - 1], window[length / 2]);
		});
	}

	public class MovingRange {
		public final float min;
		public final float max;
		public final float median;

		private MovingRange(float min, float max, float median) {
			this.min = min;
			this.max = max;
			this.median = median;
		}
	}

	public float[] reverseEma(float[] prices, double alpha) {
		var w = 8;
		var w1 = w + 1;
		var beta = 1d - alpha;
		var price0 = prices[0];

		double[] re = Doubles_.toArray(w1, i -> price0);
		double[] betas = new double[w];
		var b = beta;

		for (int i = 0; i < w; i++) {
			betas[i] = b;
			b *= b;
		}

		float[] remas = new float[prices.length];
		remas[0] = price0;

		for (int t = 1; t < prices.length; t++) {
			double[] re0 = Doubles_.toArray(w1, i -> re[i]);
			re[0] = beta * re0[0] + alpha * prices[t];
			for (int j = 0; j < w; j++)
				re[j + 1] = betas[j] * re[j] + re0[j];
			remas[t] = (float) (re[0] - alpha * re[w]);
		}

		return remas;
	}

}
