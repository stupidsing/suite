package suite.ts;

import static java.lang.Math.abs;
import static java.lang.Math.expm1;
import static java.lang.Math.log;
import static java.lang.Math.max;
import static java.lang.Math.min;
import static java.lang.Math.sqrt;
import static suite.util.Streamlet_.forInt;

import java.util.ArrayList;
import java.util.Arrays;

import primal.MoreVerbs.Read;
import primal.primitive.IntVerbs.ToInt;
import primal.primitive.adt.Floats;
import primal.primitive.adt.pair.FltObjPair;
import suite.math.linalg.CholeskyDecomposition;
import suite.math.linalg.Vector;
import suite.math.numeric.Statistic;
import suite.math.numeric.Statistic.LinearRegression;
import suite.streamlet.As;
import suite.trade.Trade_;
import suite.util.To;

public class TimeSeries {

	private CholeskyDecomposition cd = new CholeskyDecomposition();
	private Statistic stat = new Statistic();
	private Vector vec = new Vector();

	// autocorrelation function
	// "The Analysis of Time Series", Chris Chatfield
	// 2.7 Autocorrelation and the Correlogram
	public float[] acf(float[] ys, int n) {
		var length = ys.length;
		var meany = stat.mean(ys);
		var ydevs = To.vector(length, i -> ys[i] - meany);
		var acovs = To.vector(length, k -> forInt(length - k).toDouble(As.sum(i -> (ydevs[i] * ydevs[i + k]))));
		var inv = 1d / acovs[0];
		return To.vector(acovs.length, k -> acovs[k] * inv);
	}

	// Augmented Dickey-Fuller test
	public double adf(float[] ys, int tor) {
		var ydiffs = differences_(1, ys);
		var length = ys.length;
		var lr = stat.linearRegression(forInt(tor, length) //
				.map(i -> FltObjPair.of(ydiffs[i],
						// i - drift term, necessary?
						Floats.concat(Floats.of(ys[i - 1], 1f, i), Floats.of(ydiffs, i - tor, i)).toArray())));
		return lr.tStatistic()[0];
	}

	public float[] back(int index, int window, float[] fs) {
		var index1 = index + 1;
		return Arrays.copyOfRange(fs, max(0, index1 - window), index1);
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

	// Advances in Financial Machine Learning, Marcos Lopez de Prado, 5.5
	public float[] fracDiff(float[] ts, double d, int window) {
		var weights = new float[window];
		var weight = 1d;

		for (var k = 0; k < window;) {
			weights[k++] = (float) weight;
			weight *= -(d - k + 1) / k;
		}

		return To.vector(ts.length - window, i -> vec.convolute(window, ts, i, weights, window));
	}

	// epchan
	public double hurst(float[] ys, int tor) {
		var logys = To.vector(ys, Math::log);
		var tors = ToInt.array(tor, t -> t + 1);
		var logVrs = To.vector(tor, t -> {
			var diffs = dropDiff_(tors[t], logys);
			var diffs2 = To.vector(diffs, diff -> diff * diff);
			return log(stat.variance(diffs2));
		});
		var lr = stat.linearRegression(forInt(logVrs.length).map(i -> FltObjPair.of((float) log(tors[i]), vec.of(logVrs[i], 1f))));
		var beta0 = lr.coefficients[0];
		return beta0 / 2d;
	}

	// http://www.financialwisdomforum.org/gummy-stuff/hurst.htm
	public double hurstFwf(float[] ys, int tor) {
		var logys = To.vector(ys, Math::log);
		var returns0 = dropDiff_(1, logys);
		var length = returns0.length;
		var pairs = new ArrayList<FltObjPair<float[]>>();
		for (var n = 0; n < length * 3 / 4; n++) {
			var returns = Arrays.copyOfRange(returns0, n, length);
			var mv = stat.meanVariance(returns);
			var mean = mv.mean;
			var devs = To.vector(returns, r -> r - mean);
			var min = Double.MAX_VALUE;
			var max = Double.MIN_VALUE;
			var sum = 0d;
			for (var dev : devs) {
				sum += dev;
				min = min(sum, min);
				max = max(sum, max);
			}
			var x = log(returns.length);
			var y = (max - min) / mv.standardDeviation();
			pairs.add(FltObjPair.of((float) y, vec.of(x, 1d)));
		}
		return stat.linearRegression(Read.from(pairs)).coefficients[0];
	}

	public boolean isUnitRootDetected(float[] ys, int tor) {
		var tStatistic = adf(ys, tor);
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
		var length = ys.length;
		if (0 < length) {
			var logReturns = new float[length - 1];
			var f0 = ys[0];
			for (var i = 0; i < logReturns.length; i++) {
				logReturns[i] = (float) Quant.logReturn(f0, ys[i + 1]);
				f0 = ys[i + 1];
			}
			return logReturns;
		} else
			return new float[0];
	}

	public LinearRegression meanReversion(float[] ys, int tor) {
		var diffs = differences_(1, ys);

		return stat.linearRegression(forInt(tor, ys.length).map(i -> FltObjPair.of(diffs[i], vec.of(ys[i], 1f))));
	}

	public LinearRegression movingAvgMeanReversion(float[] ys, float[] movingAvg, int tor) {
		var diffs = differences_(1, ys);

		return stat.linearRegression(forInt(tor, ys.length).map(i -> FltObjPair.of(diffs[i], vec.of(movingAvg[i], 1f))));
	}

	// partial autocorrelation function
	// https://stats.stackexchange.com/questions/129052/acf-and-pacf-formula
	public float[] pacf(float[] ys, int n) {
		var acf = acf(ys, n);
		var m = To.matrix(n, n, (i, j) -> acf[abs(i - j)]);
		var acf1 = Arrays.copyOfRange(acf, 1, n - 1);
		return cd.inverseMul(m).apply(acf1);
	}

	public float[] returns(float[] fs) {
		return returns_(fs);
	}

	public ReturnsStat returnsStat(float[] prices, double deltaMs) {
		var scale = Trade_.invTradeDaysPerYear * Trade_.nTradeSecondsPerDay * 1000d / deltaMs;
		return new ReturnsStat(prices, 1d, scale);
	}

	public ReturnsStat returnsStatDaily(float[] prices) {
		var dailyInterestRate = expm1(Trade_.logRiskFreeInterestRate * Trade_.invTradeDaysPerYear);
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
			this(prices, scale, expm1(Trade_.logRiskFreeInterestRate * scale));
		}

		private ReturnsStat(float[] prices, double scale, double interestRate) {
			var length = prices.length;
			double v0, vx;
			if (0 < length) {
				v0 = prices[0];
				vx = prices[length - 1];
			} else
				v0 = vx = 1d;

			var returns_ = returns_(prices);
			var mv = stat.meanVariance(returns_);

			return_ = expm1(Quant.logReturn(v0, vx) * returns_.length * scale);
			returns = returns_;
			mean = mv.mean - interestRate;
			variance = scale * mv.variance;
		}

		public float[] returns() {
			return returns;
		}

		public double sharpeRatio() {
			return mean / sqrt(variance);
		}

		public double kellyCriterion() {
			return mean / variance;
		}
	}

	public double varianceRatio(float[] prices, int tor) {
		var logs = To.vector(prices, Math::log);
		var diffsTor = dropDiff_(tor, logs);
		var diffs1 = dropDiff_(1, logs);
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
		return differencesOn_(tor, vec.copyOf(fs));
	}

	private float[] differencesOn_(int tor, float[] fs) {
		var i = fs.length - 1;
		while (tor <= i) {
			fs[i] -= fs[i - tor];
			i--;
		}
		while (0 <= i)
			fs[i--] = 0f;
		return fs;
	}

	private float[] returns_(float[] fs) {
		var length = fs.length;
		var returns = new float[length];
		var price0 = 0 < length ? fs[0] : 0f;
		for (var i = 0; i < returns.length; i++) {
			var price = fs[i];
			returns[i] = (float) Quant.return_(price0, price);
			price0 = price;
		}
		return returns;
	}

}
