package suite.trade;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

import suite.algo.Statistic;
import suite.algo.Statistic.LinearRegression;
import suite.inspect.Dump;
import suite.math.Matrix;
import suite.math.TimeSeries;
import suite.os.LogUtil;
import suite.streamlet.Read;
import suite.trade.Hkex.Company;
import suite.util.To;

public class TradePlanTest {

	private double neglog2 = -Math.log(2d);

	private Matrix mtx = new Matrix();
	private Hkex hkex = new Hkex();
	private Yahoo yahoo = new Yahoo();
	private Statistic stat = new Statistic();
	private MovingAverage movingAvg = new MovingAverage();
	private TimeSeries ts = new TimeSeries();

	public class MeanReversionStats {
		public final float adf;
		public final float hurst;
		public final float varianceRatio;
		public final float halfLife;
		public final float movingAvgHalfLife;

		private MeanReversionStats(DataSource dataSource) {
			adf = adf(dataSource);
			hurst = hurst(dataSource);
			varianceRatio = varianceRatio(dataSource);
			halfLife = halfLife(dataSource);
			movingAvgHalfLife = movingAverageHalfLife(dataSource);
		}
	}

	@Test
	public void testStats() {
		Dump.out(new MeanReversionStats(yahoo.dataSource("0005.HK")));
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

		Map<String, MeanReversionStats> meanReversionStatsByStockCode0 = Read.from2(dataSourceByStockCode) //
				.mapValue(MeanReversionStats::new) //
				.toMap();

		// make sure all time-series are mean-reversions:
		// ensure ADF < 0f: price is not random walk
		// ensure Hurst exponent < .5f: price is weakly mean reverting
		// ensure 0f < variable ratio: statistic is significant
		// ensure 0 < half-life: determine investment period
		Map<String, MeanReversionStats> meanReversionStatsByStockCode1 = Read.from2(meanReversionStatsByStockCode0) //
				.filterValue(mr -> mr.adf < 0f //
						&& mr.hurst < .5f //
						&& 0f < mr.varianceRatio //
						&& 0f < mr.halfLife) //
				.toMap();

		Dump.out(meanReversionStatsByStockCode1);

		// filter away equities without enough price histories
		// trim the data sources to fixed sizes (128 days)?
		// make sure prices are not random walks
		// make sure prices are weakly reverting
		// make sure statistic is significant?
		// calculate moving averages
		// calculate potential return = deviation from mean * mean rev factor
		// add cash with epsilon potential return
		// find the equities with top 9 potential returns
		// TODO how to distribute your money???
		// transaction costs? Sharpe ratio? etc.
	}

	// Augmented Dickey-Fuller test
	private float adf(DataSource dataSource) {
		int tor = 16;
		float[] prices = dataSource.prices;
		float[] diffs = ts.differences(1, prices);
		float[][] deps = new float[prices.length][];
		for (int i = tor; i < deps.length; i++)
			// i - drift term, necessary?
			deps[i] = mtx.concat(new float[] { prices[i - 1], 1f, i, }, Arrays.copyOfRange(diffs, i - tor, i));
		float[][] deps1 = ts.drop(tor, deps);
		float[] diffs1 = ts.drop(tor, diffs);
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
			float[] diffs = ts.dropDiff(tors[t], logPrices);
			float[] diffs2 = To.floatArray(diffs, diff -> diff * diff);
			return (float) Math.log(stat.variance(diffs2));
		});
		float[][] deps = To.array(float[].class, logVrs.length, i -> new float[] { logVrs[i], 1f, });
		float[] n = To.floatArray(logVrs.length, i -> (float) Math.log(tors[i]));
		LinearRegression lr = stat.linearRegression(deps, n);
		float beta0 = lr.betas[0];
		return beta0 / 2f;
	}

	private float varianceRatio(DataSource dataSource) {
		int tor = 16;
		float[] prices = dataSource.prices;
		float[] logs = To.floatArray(prices, price -> (float) Math.log(price));
		float[] diffsTor = ts.dropDiff(tor, logs);
		float[] diffs1 = ts.dropDiff(1, logs);
		return stat.variance(diffsTor) / (tor * stat.variance(diffs1));
	}

	private float halfLife(DataSource dataSource) {
		int tor = 1;
		float[] prices = dataSource.prices;
		float[][] deps = To.array(float[].class, prices.length - tor, i -> new float[] { prices[i], 1f, });
		float[] diffs1 = ts.dropDiff(tor, prices);
		LinearRegression lr = stat.linearRegression(deps, diffs1);
		float beta0 = lr.betas[0];
		return (float) (neglog2 / Math.log(beta0));
	}

	private float movingAverageHalfLife(DataSource dataSource) {
		int tor = 16;
		float[] prices = dataSource.prices;
		float[] ma = ts.drop(tor, movingAvg.movingAvg(prices, tor));
		float[][] deps = To.array(float[].class, prices.length - tor, i -> new float[] { ma[i], 1f, });
		float[] diffs1 = ts.dropDiff(tor, prices);
		LinearRegression lr = stat.linearRegression(deps, diffs1);
		float beta0 = lr.betas[0];
		return (float) (neglog2 / Math.log(beta0));
	}

}
