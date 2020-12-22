package suite.trade.analysis;

import static java.lang.Math.exp;
import static java.lang.Math.log;
import static java.lang.Math.max;

import java.util.Arrays;

import primal.primitive.DblVerbs.ToDbl;
import suite.math.linalg.Vector;
import suite.ts.TimeSeries;
import suite.util.To;

public class MovingAverage {

	private TimeSeries ts = new TimeSeries();
	private Vector vec = new Vector();

	// exponential moving average convergence/divergence
	public float[] emacd(float[] prices, double alpha0, double alpha1) {
		var emas0 = exponentialMovingAvg(prices, alpha0); // long-term
		var emas1 = exponentialMovingAvg(prices, alpha1); // short-term
		return vec.sub(emas1, emas0);
	}

	// moving average convergence/divergence
	public Macd macd(float[] prices, int nDays0, int nDays1, int nDays2) {
		var mas0 = movingAvg(prices, nDays0); // long-term
		var mas1 = movingAvg(prices, nDays1); // short-term
		var diffs = vec.sub(mas1, mas0);
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

	// https://en.wikipedia.org/wiki/Exponential_smoothing#Double_exponential_smoothing
	public float[] doubleExpSmoothing_HoltWinters(float[] fs, double alpha, double beta) {
		var des = new float[fs.length + 1];
		var nalpha = 1d - alpha;
		var nbeta = 1d - beta;
		var des_ = (double) fs[0];
		var a = (double) fs[1]; // estimated level
		var b = (double) fs[1] - fs[0]; // estimated trend
		var i = 0;
		for (; i < 2; i++)
			des[i] = (float) (des_ = fs[i]);
		for (; i < fs.length; i++) {
			var a_ = a;
			var b_ = b;
			a = alpha * fs[i] + nalpha * des_;
			b = beta * (a - a_) + nbeta * b_;
			des[i] = (float) (des_ = a + b);
		}
		for (; i <= des.length; i++)
			des[i] = (float) (des_ += b); // prediction
		return des;
	}

	// https://en.wikipedia.org/wiki/Exponential_smoothing#Double_exponential_smoothing
	public float[] doubleExpSmoothing_Browns(float[] fs, double alpha) {
		var des = new float[fs.length + 1];
		var nalpha = 1d - alpha;
		var ana = alpha / nalpha;
		var des_ = (double) fs[0];
		var fs0 = 0 < fs.length ? (double) fs[0] : 0d;
		var sa = fs0;
		var sb = fs0;
		var a = sa; // estimated level
		var b = 0d; // estimated trend
		var i = 0;
		for (; i < fs.length; i++) {
			var sa_ = sa;
			var sb_ = sb;
			sa = alpha * fs[i] + nalpha * sa_;
			sb = alpha * sa + nalpha * sb_;
			a = 2 * sa - sb;
			b = ana * (sa - sb);
			des[i] = (float) (des_ = a + b);
		}
		for (; i <= des.length; i++)
			des[i] = (float) (des_ += b); // prediction
		return des;

	}

	public float[] exponentialGeometricMovingAvg(float[] prices, int halfLife) {
		return exponentialGeometricMovingAvg(prices, exp(log(.5d) * halfLife));
	}

	public float[] exponentialGeometricMovingAvg(float[] prices, double alpha) {
		var logPrices = To.vector(prices, Math::log);
		var movingAvgs = exponentialMovingAvg(logPrices, alpha);
		return To.vector(movingAvgs, Math::exp);
	}

	public float[] exponentialMovingAvg(float[] prices, int halfLife) {
		return exponentialMovingAvg(prices, exp(log(.5d) * halfLife));
	}

	public float[] exponentialMovingAvg(float[] prices, double alpha) {
		var length = prices.length;
		var emas = new float[length];
		var ema = 0 < length ? prices[0] : 0d;
		for (var day = 0; day < length; day++)
			emas[day] = (float) (ema += alpha * (prices[day] - ema));
		return emas;
	}

	public float[] geometricMovingAvg(float[] prices, int windowSize) {
		var logPrices = To.vector(prices, Math::log);
		var movingAvgs = movingAvg(logPrices, windowSize);
		return To.vector(movingAvgs, Math::exp);
	}

	// movingAvg(prices, 1) = prices
	// movingAvg(prices, 2) means taking average of two-days
	public float[] movingAvg(float[] prices, int windowSize) {
		var length = prices.length;
		var movingAvgs = new float[length];
		var div = 1d / windowSize;
		var movingSum = 0 < length ? prices[0] * windowSize : 0d;

		for (var day = 0; day < length; day++) {
			movingSum += prices[day] - prices[max(0, day - windowSize)];
			movingAvgs[day] = (float) (movingSum * div);
		}

		return movingAvgs;
	}

	public MovingRange[] movingRange(float[] prices, int windowSize) {
		return To.array(prices.length, MovingRange.class, i -> {
			var window = ts.back(i, windowSize, prices);
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

		var re = ToDbl.array(w1, i -> price0);
		var betas = new double[w];
		var b = beta;

		for (var i = 0; i < w; i++) {
			betas[i] = b;
			b *= b;
		}

		var remas = new float[prices.length];
		remas[0] = price0;

		for (var t = 1; t < prices.length; t++) {
			var re0 = ToDbl.array(w1, i -> re[i]);
			re[0] = beta * re0[0] + alpha * prices[t];
			for (var j = 0; j < w; j++)
				re[j + 1] = betas[j] * re[j] + re0[j];
			remas[t] = (float) (re[0] - alpha * re[w]);
		}

		return remas;
	}

}
