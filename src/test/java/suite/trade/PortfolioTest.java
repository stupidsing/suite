package suite.trade;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Test;

import suite.algo.Statistic;
import suite.algo.Statistic.LinearRegression;
import suite.math.MathUtil;
import suite.math.Matrix;
import suite.math.TimeSeries;
import suite.os.LogUtil;
import suite.streamlet.Read;
import suite.streamlet.Streamlet;
import suite.streamlet.Streamlet2;
import suite.trade.Hkex.Company;
import suite.util.FormatUtil;
import suite.util.To;
import suite.util.Util;

public class PortfolioTest {

	private double neglog2 = -Math.log(2d);

	private Matrix mtx = new Matrix();
	private Hkex hkex = new Hkex();
	private Yahoo yahoo = new Yahoo();
	private Statistic stat = new Statistic();
	private MovingAverage movingAvg = new MovingAverage();
	private TimeSeries ts = new TimeSeries();

	@Test
	public void testStats() {
		System.out.println(new MeanReversionStats(yahoo.dataSource("1113.HK")));
	}

	@Test
	public void testPortfolio() {
		float riskFreeInterestRate = 1.05f;
		int top = 10;

		Map<String, DataSource> dataSourceByStockCode = new HashMap<>();
		Streamlet<Company> companies = hkex.queryCompanies();

		for (Company stock : companies)
			try {
				String stockCode = stock.code;
				DataSource dataSource = yahoo.dataSource(stockCode);
				dataSource.validateTwoYears();
				dataSourceByStockCode.put(stockCode, dataSource);
			} catch (Exception ex) {
				LogUtil.warn(ex.getMessage() + " in " + stock);
			}

		Map<String, Integer> lotSizeByStockCode = hkex.queryLotSizeByStockCode(companies);

		List<String> tradeDates = Read.from2(dataSourceByStockCode) //
				.concatMap((stockCode, dataSource) -> Read.from(dataSource.dates)) //
				.distinct() //
				.toList();

		long oneYearAgo = FormatUtil.date(Util.last(tradeDates)).minusYears(-1).toEpochDay();

		int nTradeDaysInYear = Read.from(tradeDates) //
				.filter(tradeDate -> oneYearAgo < FormatUtil.date(tradeDate).toEpochDay()) //
				.size();

		Map<String, MeanReversionStats> meanReversionStatsByStockCode = Read.from2(dataSourceByStockCode) //
				.mapValue(MeanReversionStats::new) //
				.toMap();

		Map<String, Float> latestPriceByStockCode = yahoo.quote(Read.from(dataSourceByStockCode.keySet()));

		// make sure all time-series are mean-reversions:
		// ensure ADF < 0f: price is not random walk
		// ensure Hurst exponent < .5f: price is weakly mean reverting
		// ensure 0f < variable ratio: statistic is significant
		// ensure 0 < half-life: determine investment period
		Streamlet2<String, Double> potentialByStockCode = Read.from2(meanReversionStatsByStockCode) //
				.filterValue(mrs -> mrs.adf < 0f //
						&& mrs.hurst < .5f //
						&& 0f < mrs.varianceRatio //
						&& 0 < mrs.movingAvgMeanReversionRatio) //
				.map2((stockCode, mrs) -> stockCode, (stockCode, mrs) -> {
					double price = latestPriceByStockCode.get(stockCode);
					double lma = mrs.latestMovingAverage();
					double potential = (lma / price - 1d) * mrs.movingAvgMeanReversionRatio;
					System.out.println(hkex.getCompany(stockCode) //
							+ ", movingAvgMeanReversionRatio = " + mrs.movingAvgMeanReversionRatio //
							+ ", " + MathUtil.format(price) + " => " + MathUtil.format(lma) //
							+ ", potential = " + MathUtil.format(Math.pow(1d + potential, nTradeDaysInYear)));
					return potential;
				}) //
				.sortBy((stockCode, potential) -> -potential);

		System.out.println(potentialByStockCode.mapValue(MathUtil::format).toList());

		List<String> tops = potentialByStockCode //
				.filterValue(potential -> riskFreeInterestRate < potential) //
				.take(top) //
				.keys() //
				.toList();

		System.out.println(tops);

		Account account = new Account();

		for (String stockCode : tops)
			account.buySell(stockCode, lotSizeByStockCode.get(stockCode), dataSourceByStockCode.get(stockCode).get(-1).price);

		// filter away equities without enough price histories
		// conclude the trading dates
		// align and trim data source dates (128 days?)
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

	public class MeanReversionStats {
		public final float[] movingAverage;
		public final float adf;
		public final float hurst;
		public final float varianceRatio;
		public final float meanReversionRatio;
		public final float movingAvgMeanReversionRatio;
		public final float halfLife;
		public final float movingAvgHalfLife;

		private MeanReversionStats(DataSource dataSource) {
			float[] prices = dataSource.prices;
			int tor = 16;

			movingAverage = movingAvg.movingGeometricAvg(prices, tor);
			adf = adf(dataSource, tor);
			hurst = hurst(dataSource, tor);
			varianceRatio = varianceRatio(dataSource, tor);
			meanReversionRatio = meanReversionRatio(dataSource, 1);
			movingAvgMeanReversionRatio = movingAvgMeanReversionRatio(dataSource, movingAverage, tor);
			halfLife = (float) (neglog2 / Math.log1p(meanReversionRatio));
			movingAvgHalfLife = (float) (neglog2 / Math.log1p(movingAvgMeanReversionRatio));
		}

		public float latestMovingAverage() {
			return movingAverage[movingAverage.length - 1];
		}

		public String toString() {
			return "adf = " + adf //
					+ ", hurst = " + hurst //
					+ ", varianceRatio = " + varianceRatio //
					+ ", halfLife = " + halfLife //
					+ ", movingAvgHalfLife = " + movingAvgHalfLife //
					+ ", latestMovingAverage = " + latestMovingAverage();
		}
	}

	// Augmented Dickey-Fuller test
	private float adf(DataSource dataSource, int tor) {
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

	private float hurst(DataSource dataSource, int tor) {
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

	private float varianceRatio(DataSource dataSource, int tor) {
		float[] prices = dataSource.prices;
		float[] logs = To.floatArray(prices, price -> (float) Math.log(price));
		float[] diffsTor = ts.dropDiff(tor, logs);
		float[] diffs1 = ts.dropDiff(1, logs);
		return stat.variance(diffsTor) / (tor * stat.variance(diffs1));
	}

	private float meanReversionRatio(DataSource dataSource, int tor) {
		float[] prices = dataSource.prices;
		float[][] deps = To.array(float[].class, prices.length - tor, i -> new float[] { prices[i], 1f, });
		float[] diffs1 = ts.dropDiff(tor, prices);
		LinearRegression lr = stat.linearRegression(deps, diffs1);
		return lr.betas[0];
	}

	private float movingAvgMeanReversionRatio(DataSource dataSource, float[] movingAvg, int tor) {
		float[] prices = dataSource.prices;
		float[] ma = ts.drop(tor, movingAvg);
		float[][] deps = To.array(float[].class, prices.length - tor, i -> new float[] { ma[i], 1f, });
		float[] diffs1 = ts.dropDiff(tor, prices);
		LinearRegression lr = stat.linearRegression(deps, diffs1);
		return lr.betas[0];
	}

}
