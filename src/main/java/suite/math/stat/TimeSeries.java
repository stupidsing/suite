package suite.math.stat;

import java.util.Arrays;

import suite.math.linalg.Matrix;
import suite.math.stat.Statistic.LinearRegression;
import suite.math.stat.Statistic.MeanVariance;
import suite.primitive.Floats;
import suite.primitive.Floats_;
import suite.primitive.Ints_;
import suite.trade.Trade_;
import suite.util.To;

public class TimeSeries {

	private Matrix mtx = new Matrix();
	private Statistic stat = new Statistic();

	// Augmented Dickey-Fuller test
	public double adf(float[] ys, int tor) {
		float[] ydiffs = differences_(1, ys);
		float[][] xs = new float[ys.length][];
		for (int i = tor; i < xs.length; i++)
			// i - drift term, necessary?
			xs[i] = Floats.concat(Floats.of(ys[i - 1], 1f, i), Floats.of(ydiffs, i - tor, i)).toArray();
		float[][] xs1 = drop_(tor, xs);
		float[] ydiffs1 = drop_(tor, ydiffs);
		LinearRegression lr = stat.linearRegression(xs1, ydiffs1);
		return lr.tStatistic()[0];
	}

	public float[] back(int index, int window, float[] fs) {
		int index1 = index + 1;
		return Arrays.copyOfRange(fs, Math.max(0, index1 - window), index1);
	}

	public float[] differences(int tor, float[] fs) {
		return differences_(tor, fs);
	}

	public float[] differencesOn(int tor, float[] fs) {
		return differencesOn_(tor, fs);
	}

	public float[] drop(int tor, float[] fs) {
		return drop_(tor, fs);
	}

	public float[][] drop(int tor, float[][] fs) {
		return drop_(tor, fs);
	}

	public float[] dropDiff(int tor, float[] fs) {
		return drop_(tor, differences_(tor, fs));
	}

	public double hurst(float[] ys, int tor) {
		float[] logys = To.arrayOfFloats(ys, price -> (float) Math.log(price));
		int[] tors = Ints_.toArray(tor, t -> t + 1);
		float[] logVrs = Floats_.toArray(tor, t -> {
			float[] diffs = dropDiff(tors[t], logys);
			float[] diffs2 = To.arrayOfFloats(diffs, diff -> diff * diff);
			return (float) Math.log(stat.variance(diffs2));
		});
		float[][] xs = To.array(float[].class, logVrs.length, i -> new float[] { logVrs[i], 1f, });
		float[] n = Floats_.toArray(logVrs.length, i -> (float) Math.log(tors[i]));
		LinearRegression lr = stat.linearRegression(xs, n);
		float beta0 = lr.coefficients[0];
		return beta0 / 2d;
	}

	public boolean isUnitRootDetected(float[] ys, int tor) {
		double tStatistic = adf(ys, tor);
		if (ys.length <= 25)
			return -3d <= tStatistic;
		else if (ys.length <= 50)
			return -2.93d <= tStatistic;
		else if (ys.length <= 100)
			return -2.89d <= tStatistic;
		else if (ys.length <= 250)
			return -2.88d <= tStatistic;
		else if (ys.length <= 500)
			return -2.87d <= tStatistic;
		else
			return -2.86d <= tStatistic;
	}

	public float[] logReturns(float[] ys) {
		int length = ys.length;
		if (0 < length) {
			float[] logReturns = new float[length - 1];
			float f0 = ys[0];
			for (int i = 0; i < logReturns.length; i++) {
				logReturns[i] = (float) Quant.logReturn(f0, ys[i + 1]);
				f0 = ys[i + 1];
			}
			return logReturns;
		} else
			return new float[0];
	}

	public LinearRegression meanReversion(float[] ys, int tor) {
		float[][] xs = To.array(float[].class, ys.length - tor, i -> new float[] { ys[i], 1f, });
		float[] diffs1 = drop_(tor, differences_(1, ys));
		return stat.linearRegression(xs, diffs1);
	}

	public LinearRegression movingAvgMeanReversion(float[] ys, float[] movingAvg, int tor) {
		float[] ma = drop_(tor, movingAvg);
		float[][] xs = To.array(float[].class, ys.length - tor, i -> new float[] { ma[i], 1f, });
		float[] diffs1 = drop_(tor, differences_(1, ys));
		return stat.linearRegression(xs, diffs1);
	}

	public float[] returns(float[] fs) {
		return returns_(fs);
	}

	public ReturnsStat returnsStat(float[] prices, double deltaMs) {
		double scale = Trade_.invTradeDaysPerYear * Trade_.nTradeSecondsPerDay * 1000d / deltaMs;
		return new ReturnsStat(prices, 1d, scale);
	}

	public ReturnsStat returnsStatDaily(float[] prices) {
		double dailyInterestRate = Math.expm1(Trade_.logRiskFreeInterestRate * Trade_.invTradeDaysPerYear);
		return new ReturnsStat(prices, 1d, dailyInterestRate);
	}

	public ReturnsStat returnsStatDailyAnnualized(float[] prices) {
		return new ReturnsStat(prices, Trade_.invTradeDaysPerYear);
	}

	public class ReturnsStat {
		public final double return_;
		private float[] returns;
		private double mean;
		private double variance;

		private ReturnsStat(float[] prices, double scale) {
			this(prices, scale, Math.expm1(Trade_.logRiskFreeInterestRate * scale));
		}

		private ReturnsStat(float[] prices, double scale, double interestRate) {
			int length = prices.length;
			double v0, vx;
			if (0 < length) {
				v0 = prices[0];
				vx = prices[length - 1];
			} else
				v0 = vx = 1d;

			float[] returns_ = returns_(prices);
			MeanVariance mv = stat.meanVariance(returns_);

			return_ = Math.expm1(Quant.logReturn(v0, vx) * returns_.length * scale);
			returns = returns_;
			mean = mv.mean - interestRate;
			variance = scale * mv.variance;
		}

		public float[] returns() {
			return returns;
		}

		public double sharpeRatio() {
			return mean / Math.sqrt(variance);
		}

		public double kellyCriterion() {
			return mean / variance;
		}
	}

	public double varianceRatio(float[] prices, int tor) {
		float[] logs = To.arrayOfFloats(prices, price -> (float) Math.log(price));
		float[] diffsTor = dropDiff(tor, logs);
		float[] diffs1 = dropDiff(1, logs);
		return stat.variance(diffsTor) / (tor * stat.variance(diffs1));
	}

	private float[] drop_(int tor, float[] fs) {
		return Arrays.copyOfRange(fs, tor, fs.length);
	}

	private float[][] drop_(int tor, float[][] fs) {
		return Arrays.copyOfRange(fs, tor, fs.length);
	}

	private float[] differences_(int tor, float[] fs) {
		return differencesOn_(tor, mtx.of(fs));
	}

	private float[] differencesOn_(int tor, float[] fs) {
		int i = fs.length;
		while (tor <= --i)
			fs[i] -= fs[i - tor];
		while (0 <= --i)
			fs[i] = 0f;
		return fs;
	}

	private float[] returns_(float[] fs) {
		int length = fs.length;
		float[] returns = new float[length];
		float price0 = 0 < length ? fs[0] : 0f;
		for (int i = 0; i < returns.length; i++) {
			float price = fs[i];
			returns[i] = (float) Quant.return_(price0, price);
			price0 = price;
		}
		return returns;
	}

}
