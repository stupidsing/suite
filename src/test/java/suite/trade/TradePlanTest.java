package suite.trade;

import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

import suite.algo.Statistic;
import suite.os.LogUtil;
import suite.streamlet.Read;
import suite.trade.Hkex.Company;
import suite.util.To;
import suite.util.Util;

public class TradePlanTest {

	private Hkex hkex = new Hkex();
	private Yahoo yahoo = new Yahoo();
	private Statistic stat = new Statistic();

	public class MeanReversionStats {
		public final float hurst;
		public final float varianceRatio;
		public final float halfLife;

		private MeanReversionStats(DataSource dataSource) {
			hurst = hurst(dataSource);
			varianceRatio = varianceRatio(dataSource);
			halfLife = halfLife(dataSource);
		}
	}

	@Test
	public void testTradePlan() {
		Map<String, DataSource> dataSourceByStockCode = new HashMap<>();

		for (Company stock : hkex.companies.take(5))
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
		// calculate ADF < 0f
		// calculate Hurst exponent < .5f
		// calculate 0f < variable ratio
		// calculate 0 < half-life

		Util.dump(meanReversionStatsByStockCode);
	}

	private float hurst(DataSource dataSource) {
		int tor = 1;
		float[] logs = To.floatArray(dataSource.prices.length, i -> (float) Math.log(dataSource.prices[i]));
		float[] diffsTor = differences(logs, tor);
		float[] vr = To.floatArray(diffsTor.length, i -> {
			float diff = diffsTor[i];
			return diff * diff;
		});
		float[][] deps = To.array(float[].class, vr.length, i -> new float[] { vr[i], 1f, });
		float[] n = To.floatArray(vr.length, i -> i);
		return stat.linearRegression(deps, n)[0] / 2f;
	}

	private float varianceRatio(DataSource dataSource) {
		int tor = 1;
		float[] logs = To.floatArray(dataSource.prices.length, i -> (float) Math.log(dataSource.prices[i]));
		float[] diffsTor = differences(logs, tor);
		float[] diffs1 = differences(logs, 1);
		return stat.variance(diffsTor) / (tor * stat.variance(diffs1));
	}

	private float halfLife(DataSource dataSource) {
		float[] prices = dataSource.prices;
		float[][] deps = To.array(float[].class, prices.length - 1, i -> new float[] { prices[i], 1f, });
		float[] diffs1 = differences(prices, 1);

		float beta = stat.linearRegression(deps, diffs1)[0];
		return (float) (-Math.log(2) / Math.log(beta));
	}

	private float[] differences(float[] logs, int tor) {
		return To.floatArray(logs.length - tor, i -> logs[i + tor] - logs[i]);
	}

}
