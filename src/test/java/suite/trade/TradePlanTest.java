package suite.trade;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

import suite.algo.Statistic;
import suite.algo.Statistic.LinearRegression;
import suite.math.Matrix;
import suite.math.TimeSeries;
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
	private TimeSeries ts = new TimeSeries();

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

	// Augmented Dickey-Fuller test
	private float adf(DataSource dataSource) {
		int tor = 16;
		float[] prices = dataSource.prices;
		float[] diffs = ts.differences(prices, 1);
		float[][] deps = new float[prices.length][];
		for (int i = tor; i < deps.length; i++)
			// i - drift term, necessary?
			deps[i] = mtx.concat(new float[] { prices[i - 1], 1f, i, }, Arrays.copyOfRange(diffs, i - tor, i));
		float[][] deps1 = ts.drop(deps, tor);
		float[] diffs1 = ts.drop(diffs, tor);
		LinearRegression lr = stat.linearRegression(deps1, diffs1);
		float lambda = lr.betas[0];
		return (float) (lambda / lr.standardError);
	}

	private float hurst(DataSource dataSource) {
		int tor = 16;
		float[] prices = dataSource.prices;
		float[] logPrices = To.floatArray(prices, price -> (float) Math.log(price));
		int[] tors = To.intArray(tor, t -> t + 1);
		float[] logVrs = To.floatArray(tor, t -> {
			float[] diffs = ts.dropDiff(logPrices, tors[t]);
			float[] diffs2 = To.floatArray(diffs, diff -> diff * diff);
			return (float) Math.log(stat.variance(diffs2));
		});
		float[][] deps = To.array(float[].class, logVrs.length, i -> new float[] { logVrs[i], 1f, });
		float[] n = To.floatArray(logVrs.length, i -> (float) Math.log(tors[i]));
		LinearRegression lr = stat.linearRegression(deps, n);
		float[] ps = lr.betas;
		float beta = ps[0];
		return beta / 2f;
	}

	private float varianceRatio(DataSource dataSource) {
		int tor = 16;
		float[] prices = dataSource.prices;
		float[] logs = To.floatArray(prices, price -> (float) Math.log(price));
		float[] diffsTor = ts.dropDiff(logs, tor);
		float[] diffs1 = ts.dropDiff(logs, 1);
		return stat.variance(diffsTor) / (tor * stat.variance(diffs1));
	}

	private float halfLife(DataSource dataSource) {
		int tor = 1;
		float[] prices = dataSource.prices;
		float[][] deps = To.array(float[].class, prices.length - tor, i -> new float[] { prices[i], 1f, });
		float[] diffs1 = ts.dropDiff(prices, tor);
		LinearRegression lr = stat.linearRegression(deps, diffs1);
		float[] ps = lr.betas;
		float beta = ps[0];
		return (float) (-Math.log(2) / Math.log(beta));
	}

}
