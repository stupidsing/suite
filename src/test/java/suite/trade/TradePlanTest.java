package suite.trade;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

import suite.algo.Statistic;
import suite.algo.Statistic.LinearRegression;
import suite.math.Matrix;
import suite.os.LogUtil;
import suite.streamlet.Read;
import suite.trade.Hkex.Company;
import suite.util.To;
import suite.util.Util;

public class TradePlanTest {

	private Matrix mtx = new Matrix();
	private Hkex hkex = new Hkex();
	private Yahoo yahoo = new Yahoo();
	private Statistic stat = new Statistic();

	public class MeanReversionStats {
		public final float adf;
		public final float hurst;
		public final float varianceRatio;
		public final float halfLife;

		private MeanReversionStats(DataSource dataSource) {
			adf = adf(dataSource);
			hurst = hurst(dataSource);
			varianceRatio = varianceRatio(dataSource);
			halfLife = halfLife(dataSource);
		}
	}

	@Test
	public void testStats() {
		Util.dump(new MeanReversionStats(yahoo.dataSource("0005.HK")));
	}

	@Test
	public void testTradePlan() {
		Map<String, DataSource> dataSourceByStockCode = new HashMap<>();

		for (Company stock : hkex.queryCompanies().take(5))
			try {
				String stockCode = stock.code;
				dataSourceByStockCode.put(stockCode, yahoo.dataSource(stockCode));
			} catch (Exception ex) {
				LogUtil.warn(ex.getMessage() + " in " + stock);
			}

		Map<String, MeanReversionStats> meanReversionStatsByStockCode = Read.from2(dataSourceByStockCode) //
				.mapValue(MeanReversionStats::new) //
				.toMap();

		// make sure all time-series are mean-reversions:
		// calculate ADF < 0f to make sure data is not random walk
		// calculate Hurst exponent < .5f
		// calculate 0f < variable ratio
		// calculate 0 < half-life

		Util.dump(meanReversionStatsByStockCode);
	}

	// Augmented Dickey-Fuller
	private float adf(DataSource dataSource) {
		int tor = 16;
		float[] prices = dataSource.prices;
		float[] diffs = differences(prices, 1);
		float[][] deps = new float[prices.length][];
		for (int i = tor; i < deps.length; i++)
			// i - drift term, necessary?
			deps[i] = mtx.concat(new float[] { prices[i - 1], 1f, i, }, Arrays.copyOfRange(diffs, i - tor, i));
		float[][] deps1 = drop(deps, tor);
		float[] diffs1 = drop(diffs, tor);
		LinearRegression lr = stat.linearRegression(deps1, diffs1);
		float lambda = lr.betas[0];
		return (float) (lambda / lr.standardError);
	}

	private float hurst(DataSource dataSource) {
		int tor = 16;
		float[] prices = dataSource.prices;
		float[] logs = To.floatArray(prices.length, i -> (float) Math.log(prices[i]));
		float[] diffsTor = dropDiff(logs, tor);
		float[] vr = To.floatArray(diffsTor.length, i -> {
			float diff = diffsTor[i];
			return diff * diff;
		});
		float[][] deps = To.array(float[].class, vr.length, i -> new float[] { vr[i], 1f, });
		float[] n = To.floatArray(vr.length, i -> i);
		LinearRegression lr = stat.linearRegression(deps, n);
		float[] ps = lr.betas;
		return ps[0] / 2f;
	}

	private float varianceRatio(DataSource dataSource) {
		int tor = 16;
		float[] prices = dataSource.prices;
		float[] logs = To.floatArray(prices.length, i -> (float) Math.log(prices[i]));
		float[] diffsTor = dropDiff(logs, tor);
		float[] diffs1 = dropDiff(logs, 1);
		return stat.variance(diffsTor) / (tor * stat.variance(diffs1));
	}

	private float halfLife(DataSource dataSource) {
		int tor = 1;
		float[] prices = dataSource.prices;
		float[][] deps = To.array(float[].class, prices.length - tor, i -> new float[] { prices[i], 1f, });
		float[] diffs1 = dropDiff(prices, tor);
		LinearRegression lr = stat.linearRegression(deps, diffs1);
		float[] ps = lr.betas;
		float beta = ps[0];
		return (float) (-Math.log(2) / Math.log(beta));
	}

	private float[] dropDiff(float[] logs, int tor) {
		return drop(differences(logs, tor), tor);
	}

	private float[] differences(float[] fs, int tor) {
		return differencesOn(mtx.of(fs), tor);
	}

	private float[] differencesOn(float[] fs, int tor) {
		int i = fs.length;
		while (tor <= --i)
			fs[i] -= fs[i - tor];
		while (0 <= --i)
			fs[i] = 0f;
		return fs;
	}

	private float[] drop(float[] fs, int tor) {
		return Arrays.copyOfRange(fs, tor, fs.length);
	}

	private float[][] drop(float[][] fs, int tor) {
		return Arrays.copyOfRange(fs, tor, fs.length);
	}

}
