package suite.math.stat;

import java.util.Arrays;

import suite.math.linalg.Matrix;
import suite.math.stat.Statistic.LinearRegression;
import suite.math.stat.Statistic.MeanVariance;
import suite.trade.Trade_;
import suite.util.Copy;
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
			xs[i] = mtx.concat(new float[] { ys[i - 1], 1f, i, }, Arrays.copyOfRange(ydiffs, i - tor, i));
		float[][] xs1 = drop_(tor, xs);
		float[] ydiffs1 = drop_(tor, ydiffs);
		LinearRegression lr = stat.linearRegression(xs1, ydiffs1);
		return lr.tStatistic()[0];
	}

	public LinearRegression ar(float[] ys, int n) {
		int length = ys.length;
		float[][] deps = To.array(float[].class, length - n, i -> Arrays.copyOfRange(ys, i, i + n));
		float[] ys1 = Arrays.copyOfRange(ys, n, length);
		return stat.linearRegression(deps, ys1);
	}

	public LinearRegression arima(float[] ys, int p, int d, int q) {
		float[] is = mtx.of(ys);
		for (int i = 0; i < d; i++)
			is = differencesOn_(i, is);
		return arma(ys, p, q);
	}

	public LinearRegression arma(float[] ys, int p, int q) {
		int length = ys.length;
		float[] residuals = new float[q];
		LinearRegression lr = null;

		for (int iter = 0; iter < q; iter++) {
			int iter_ = iter;
			float yiter = ys[iter_];
			int iter1 = iter_ - 1;

			for (int j = 0; j < Math.min(p, iter1); j++)
				yiter -= lr.coefficients[j] * ys[iter1 - j];
			for (int j = 0; j < Math.min(q, iter1); j++)
				yiter -= lr.coefficients[p + j] * residuals[iter1 - j];

			residuals[iter_] = yiter;

			float[][] xs = To.array(float[].class, length, i -> {
				int from = i - p;
				float[] fs1 = new float[p + iter_ + 1];
				int p0 = -Math.max(0, from);
				Arrays.fill(fs1, 0, p0, 0f);
				Copy.floats(ys, 0, fs1, p0, i - p0);
				Copy.floats(residuals, 0, fs1, p, iter_ + 1);
				return fs1;
			});

			lr = stat.linearRegression(xs, ys);
		}

		return lr;
	}

	public float[] arch(float[] ys, int p, int q) {

		// auto regressive
		int length = ys.length;
		float[][] xs0 = To.array(float[].class, length, i -> copyPadZeroes(ys, i - p, i));
		LinearRegression lr0 = stat.linearRegression(xs0, ys);

		float[] variances = To.arrayOfFloats(length, i -> {
			double residual = ys[i] - lr0.predict(xs0[i]);
			return (float) (residual * residual);
		});

		// conditional heteroskedasticity
		float[][] xs1 = To.array(float[].class, length, i -> copyPadZeroes(variances, i - p, i));
		LinearRegression lr1 = stat.linearRegression(xs1, variances);

		return mtx.concat(lr0.coefficients, lr1.coefficients);
	}

	private float[] copyPadZeroes(float[] fs0, int from, int to) {
		float[] fs1 = new float[to - from];
		int p = -Math.max(0, from);
		Arrays.fill(fs1, 0, p, 0f);
		Copy.floats(fs0, 0, fs1, p, to - p);
		return fs1;
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

	public double hurst(float[] ys, int tor) {
		float[] logys = To.arrayOfFloats(ys, price -> (float) Math.log(price));
		int[] tors = To.arrayOfInts(tor, t -> t + 1);
		float[] logVrs = To.arrayOfFloats(tor, t -> {
			float[] diffs = dropDiff(tors[t], logys);
			float[] diffs2 = To.arrayOfFloats(diffs, diff -> diff * diff);
			return (float) Math.log(stat.variance(diffs2));
		});
		float[][] xs = To.array(float[].class, logVrs.length, i -> new float[] { logVrs[i], 1f, });
		float[] n = To.arrayOfFloats(logVrs.length, i -> (float) Math.log(tors[i]));
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
				logReturns[i] = (float) Math.log1p((ys[i + 1] - f0) / f0);
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

			return_ = Math.expm1(Math.log(vx / v0) * returns_.length * scale);
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
