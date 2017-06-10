package suite.math.stat;

import java.util.Arrays;

import suite.math.linalg.Matrix;
import suite.math.stat.Statistic.LinearRegression;
import suite.math.stat.Statistic.MeanVariance;
import suite.trade.Trade_;
import suite.util.To;

public class TimeSeries {

	private Matrix mtx = new Matrix();
	private Statistic stat = new Statistic();

	// Augmented Dickey-Fuller test
	public double adf(float[] prices, int tor) {
		float[] diffs = differences_(1, prices);
		float[][] deps = new float[prices.length][];
		for (int i = tor; i < deps.length; i++)
			// i - drift term, necessary?
			deps[i] = mtx.concat(new float[] { prices[i - 1], 1f, i, }, Arrays.copyOfRange(diffs, i - tor, i));
		float[][] deps1 = drop_(tor, deps);
		float[] diffs1 = drop_(tor, diffs);
		LinearRegression lr = stat.linearRegression(deps1, diffs1);
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

	public Donchian donchian(int tor, float[] fs) {
		return new Donchian(tor, fs);
	}

	public class Donchian {
		public final float[] mins;
		public final float[] maxs;

		private Donchian(int tor, float[] fs) {
			float min = fs[0], max = fs[0];
			int length = fs.length;
			mins = new float[length];
			maxs = new float[length];
			for (int i = 0; i < length; i++) {
				float f = fs[i];
				mins[i] = Math.min(min, f);
				maxs[i] = Math.max(max, f);
			}
		}
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

	public double hurst(float[] prices, int tor) {
		float[] logPrices = To.arrayOfFloats(prices, price -> (float) Math.log(price));
		int[] tors = To.arrayOfInts(tor, t -> t + 1);
		float[] logVrs = To.arrayOfFloats(tor, t -> {
			float[] diffs = dropDiff(tors[t], logPrices);
			float[] diffs2 = To.arrayOfFloats(diffs, diff -> diff * diff);
			return (float) Math.log(stat.variance(diffs2));
		});
		float[][] deps = To.array(float[].class, logVrs.length, i -> new float[] { logVrs[i], 1f, });
		float[] n = To.arrayOfFloats(logVrs.length, i -> (float) Math.log(tors[i]));
		LinearRegression lr = stat.linearRegression(deps, n);
		float beta0 = lr.coefficients[0];
		return beta0 / 2d;
	}

	public boolean isUnitRootDetected(float[] prices, int tor) {
		double tStatistic = adf(prices, tor);
		if (prices.length <= 25)
			return -3d <= tStatistic;
		else if (prices.length <= 50)
			return -2.93d <= tStatistic;
		else if (prices.length <= 100)
			return -2.89d <= tStatistic;
		else if (prices.length <= 250)
			return -2.88d <= tStatistic;
		else if (prices.length <= 500)
			return -2.87d <= tStatistic;
		else
			return -2.86d <= tStatistic;
	}

	public float[] logReturns(float[] fs) {
		int length = fs.length;
		if (0 < length) {
			float[] logReturns = new float[length - 1];
			float f0 = fs[0];
			for (int i = 0; i < logReturns.length; i++) {
				logReturns[i] = (float) Math.log1p((fs[i + 1] - f0) / f0);
				f0 = fs[i + 1];
			}
			return logReturns;
		} else
			return new float[0];
	}

	public LinearRegression meanReversion(float[] prices, int tor) {
		float[][] deps = To.array(float[].class, prices.length - tor, i -> new float[] { prices[i], 1f, });
		float[] diffs1 = drop_(tor, differences_(1, prices));
		return stat.linearRegression(deps, diffs1);
	}

	public LinearRegression movingAvgMeanReversion(float[] prices, float[] movingAvg, int tor) {
		float[] ma = drop_(tor, movingAvg);
		float[][] deps = To.array(float[].class, prices.length - tor, i -> new float[] { ma[i], 1f, });
		float[] diffs1 = drop_(tor, differences_(1, prices));
		return stat.linearRegression(deps, diffs1);
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

			return_ = Math.expm1(Math.log(vx / v0) * scale / returns_.length);
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
		if (0 < length) {
			float[] returns = new float[length - 1];
			float price0 = fs[0];
			for (int i = 0; i < returns.length; i++) {
				float price = fs[i + 1];
				returns[i] = (price - price0) / price0;
				price0 = price;
			}
			return returns;
		} else
			return new float[0];
	}

}
