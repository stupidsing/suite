package suite.math;

import java.util.Arrays;

import suite.algo.Statistic;

public class TimeSeries {

	private Matrix mtx = new Matrix();
	private Statistic stat = new Statistic();

	public double sharpeRatio(float[] fs, double nYears) {
		return sharpeRatioKellyCriterion(fs, nYears)[0];
	}

	public double[] sharpeRatioKellyCriterion(float[] fs, double nYears) {
		float[] returns = returns(fs);
		double r0 = Math.expm1(Math.log1p(stat.riskFreeInterestRate) * nYears / returns.length);
		double mean = stat.mean(returns) - r0;
		double nYearsVariance = nYears * stat.variance(returns);
		double sharpe = mean / Math.sqrt(nYearsVariance);
		double kelly = mean / nYearsVariance;
		return new double[] { sharpe, kelly, };
	}

	public float[] logReturns(float[] fs) {
		float[] logReturns = new float[fs.length - 1];
		double ln0 = Math.log(fs[0]);
		for (int i = 0; i < logReturns.length; i++) {
			double ln = Math.log(fs[i + 1]);
			logReturns[i] = (float) (ln - ln0);
			ln0 = ln;
		}
		return logReturns;
	}

	public float[] returns(float[] fs) {
		float[] returns = new float[fs.length - 1];
		float price0 = fs[0];
		for (int i = 0; i < returns.length; i++) {
			float price = fs[i + 1];
			returns[i] = (price - price0) / price0;
			price0 = price;
		}
		return returns;
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
		return Arrays.copyOfRange(fs, tor, fs.length);
	}

	public float[] dropDiff(int tor, float[] logs) {
		return drop_(tor, differences_(tor, logs));
	}

	private float[] drop_(int tor, float[] fs) {
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

}
