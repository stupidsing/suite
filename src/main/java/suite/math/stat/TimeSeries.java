package suite.math.stat;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import suite.math.linalg.CholeskyDecomposition;
import suite.math.linalg.Vector_;
import suite.math.stat.Statistic.LinearRegression;
import suite.math.stat.Statistic.MeanVariance;
import suite.primitive.Floats;
import suite.primitive.Floats_;
import suite.primitive.Int_Dbl;
import suite.primitive.Ints_;
import suite.primitive.adt.pair.FltObjPair;
import suite.trade.Trade_;
import suite.util.To;

public class TimeSeries {

	private CholeskyDecomposition cd = new CholeskyDecomposition();
	private Statistic stat = new Statistic();
	private Vector_ vec = new Vector_();

	// autocorrelation function
	public float[] acf(float[] ys, int n) {
		double meany = stat.mean(ys);
		int length = ys.length;
		float[] ydevs = Floats_.toArray(length, i -> (float) (ys[i] - meany));
		double avgydev0 = Ints_.range(length).toDouble(Int_Dbl.sum(i -> ydevs[i])) / length;
		return Floats_.toArray(n, k -> {
			int lk = length - k;
			double nom = Ints_.range(lk).toDouble(Int_Dbl.sum(i -> ydevs[i] * ydevs[i + k]));
			double avgydev1 = Ints_.range(lk).toDouble(Int_Dbl.sum(i -> ydevs[i])) / lk;
			double denom = Math.sqrt(avgydev0 * avgydev1) * lk;
			return (float) (nom / denom);
		});
	}

	// Augmented Dickey-Fuller test
	public double adf(float[] ys, int tor) {
		float[] ydiffs = differences_(1, ys);
		int length = ys.length;
		LinearRegression lr = stat.linearRegression(Ints_ //
				.range(tor, length) //
				.map(i -> FltObjPair.of(ydiffs[i],
						// i - drift term, necessary?
						Floats.concat(Floats.of(ys[i - 1], 1f, i), Floats.of(ydiffs, i - tor, i)).toArray())) //
				.toList());
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
		return dropDiff_(tor, fs);
	}

	// epchan
	public double hurst(float[] ys, int tor) {
		float[] logys = To.arrayOfFloats(ys, price -> (float) Math.log(price));
		int[] tors = Ints_.toArray(tor, t -> t + 1);
		float[] logVrs = Floats_.toArray(tor, t -> {
			float[] diffs = dropDiff_(tors[t], logys);
			float[] diffs2 = To.arrayOfFloats(diffs, diff -> diff * diff);
			return (float) Math.log(stat.variance(diffs2));
		});
		LinearRegression lr = stat.linearRegression(Ints_ //
				.range(logVrs.length) //
				.map(i -> FltObjPair.of((float) Math.log(tors[i]), new float[] { logVrs[i], 1f, })) //
				.toList());
		float beta0 = lr.coefficients[0];
		return beta0 / 2d;
	}

	// http://www.financialwisdomforum.org/gummy-stuff/hurst.htm
	public double hurstFwf(float[] ys, int tor) {
		float[] logys = To.arrayOfFloats(ys, price -> (float) Math.log(price));
		float[] returns0 = dropDiff_(1, logys);
		int length = returns0.length;
		List<FltObjPair<float[]>> pairs = new ArrayList<>();
		for (int n = 0; n < length * 3 / 4; n++) {
			float[] returns = Arrays.copyOfRange(returns0, n, length);
			MeanVariance mv = stat.meanVariance(returns);
			double mean = mv.mean;
			float[] devs = To.arrayOfFloats(returns, r -> (float) (r - mean));
			double min = Double.MAX_VALUE;
			double max = Double.MIN_VALUE;
			double sum = 0d;
			for (float dev : devs) {
				sum += dev;
				min = Math.min(sum, min);
				max = Math.max(sum, max);
			}
			double x = Math.log(returns.length);
			double y = (max - min) / mv.standardDeviation();
			pairs.add(FltObjPair.of((float) y, new float[] { (float) x, 1f, }));
		}
		return stat.linearRegression(pairs).coefficients[0];
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
		float[] diffs = differences_(1, ys);

		return stat.linearRegression(Ints_ //
				.range(tor, ys.length) //
				.map(i -> FltObjPair.of(diffs[i], new float[] { ys[i], 1f, })) //
				.toList());
	}

	public LinearRegression movingAvgMeanReversion(float[] ys, float[] movingAvg, int tor) {
		float[] diffs = differences_(1, ys);

		return stat.linearRegression(Ints_ //
				.range(tor, ys.length) //
				.map(i -> FltObjPair.of(diffs[i], new float[] { movingAvg[i], 1f, })) //
				.toList());
	}

	// partial autocorrelation function
	// https://stats.stackexchange.com/questions/129052/acf-and-pacf-formula
	public float[] pacf(float[] ys, int n) {
		float[] acf = acf(ys, n);
		float[][] m = To.arrayOfFloats(n, n, (i, j) -> acf[Math.abs(i - j)]);
		float[] acf1 = Arrays.copyOfRange(acf, 1, n - 1);
		return cd.inverseMul(m).apply(acf1);
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
		float[] diffsTor = dropDiff_(tor, logs);
		float[] diffs1 = dropDiff_(1, logs);
		return stat.variance(diffsTor) / (tor * stat.variance(diffs1));
	}

	private float[] dropDiff_(int tor, float[] fs) {
		return drop_(tor, differences_(tor, fs));
	}

	private float[] drop_(int tor, float[] fs) {
		return Arrays.copyOfRange(fs, tor, fs.length);
	}

	private float[][] drop_(int tor, float[][] fs) {
		return Arrays.copyOfRange(fs, tor, fs.length);
	}

	private float[] differences_(int tor, float[] fs) {
		return differencesOn_(tor, vec.of(fs));
	}

	private float[] differencesOn_(int tor, float[] fs) {
		int i = fs.length - 1;
		while (tor <= i) {
			float f0 = fs[i - tor];
			fs[i--] -= f0;
		}
		while (0 <= i)
			fs[i--] = 0f;
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
