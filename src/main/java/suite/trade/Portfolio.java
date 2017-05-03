package suite.trade;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

import suite.algo.Statistic;
import suite.algo.Statistic.LinearRegression;
import suite.math.MathUtil;
import suite.math.Matrix;
import suite.math.TimeSeries;
import suite.os.LogUtil;
import suite.streamlet.As;
import suite.streamlet.Read;
import suite.streamlet.Streamlet;
import suite.util.FormatUtil;
import suite.util.To;
import suite.util.Util;

public class Portfolio {

	private float riskFreeInterestRate = 1.05f;
	private int top = 10;
	private int tor = 16;

	private double neglog2 = -Math.log(2d);

	private Matrix mtx = new Matrix();
	private Statistic stat = new Statistic();
	private MovingAverage movingAvg = new MovingAverage();
	private TimeSeries ts = new TimeSeries();

	private Hkex hkex = new Hkex();
	private Hkex2012 hkex2012 = new Hkex2012();
	private Yahoo yahoo = new Yahoo();

	public float simulate(float valuation0, Predicate<LocalDate> datePred) {
		float valuation = valuation0;
		Map<String, DataSource> dataSourceByStockCode = new HashMap<>();
		Streamlet<Asset> assets = hkex2012.queryLeadingCompaniesByMarketCapitalisation();
		// hkex.getCompanies();

		Map<String, Integer> lotSizeByStockCode = hkex.queryLotSizeByStockCode(assets);

		for (Asset asset : assets)
			if (lotSizeByStockCode.containsKey(asset.code))
				try {
					String stockCode = asset.code;
					DataSource dataSource = yahoo.dataSourceWithLatestQuote(stockCode);
					dataSource.validate();
					dataSourceByStockCode.put(stockCode, dataSource);
				} catch (Exception ex) {
					LogUtil.warn(ex.getMessage() + " in " + asset);
				}

		List<Long> tradeEpochDays = Read.from2(dataSourceByStockCode) //
				.concatMap((stockCode, dataSource) -> Read.from(dataSource.dates)) //
				.distinct() //
				.map(tradeDate -> FormatUtil.date(tradeDate).toEpochDay()) //
				.sort(Util::compare) //
				.toList();

		long frEpochDay = Util.first(tradeEpochDays);
		long toEpochDay = Util.last(tradeEpochDays);
		Account account = new Account(valuation0);

		for (long backTestEpochDay = frEpochDay; backTestEpochDay < toEpochDay; backTestEpochDay++)
			if (datePred.test(LocalDate.ofEpochDay(backTestEpochDay))) {
				LocalDate historyWindowFrom = LocalDate.ofEpochDay(backTestEpochDay - 1024);
				LocalDate historyWindowTo = LocalDate.ofEpochDay(backTestEpochDay);
				DatePeriod historyPeriod = DatePeriod.of(historyWindowFrom, historyWindowTo);

				Map<String, DataSource> backTestDataSourceByStockCode = Read.from2(dataSourceByStockCode) //
						.mapValue(dataSource -> dataSource.limit(historyPeriod)) //
						.filterValue(dataSource -> 128 <= dataSource.dates.length) //
						.toMap();

				Map<String, Integer> portfolio = formPortfolio( //
						backTestDataSourceByStockCode, //
						lotSizeByStockCode, //
						tradeEpochDays, //
						backTestEpochDay, //
						valuation0);

				Map<String, Float> latestPriceByStockCode = Read.from2(backTestDataSourceByStockCode) //
						.mapValue(dataSource -> dataSource.get(-1).price) //
						.toMap();

				String actions = account.portfolio(portfolio, latestPriceByStockCode);
				account.validate();

				float valuation1 = valuation = valuation(account, latestPriceByStockCode);

				System.out.println(FormatUtil.formatDate(LocalDate.ofEpochDay(backTestEpochDay)) //
						+ ", valuation = " + valuation1 //
						+ ", portfolio = " + portfolio //
						+ ", actions = " + actions);
			}

		return valuation;
	}

	private float valuation(Account account, Map<String, Float> latestPriceByStockCode) {
		float v0 = account.cash();
		float v1 = Read.from2(account.assets()) //
				.collect(As.<String, Integer> sumOfFloats((stockCode, n) -> latestPriceByStockCode.get(stockCode) * n));
		float valuation = v0 + v1;
		return valuation;
	}

	private Map<String, Integer> formPortfolio( //
			Map<String, DataSource> dataSourceByStockCode, //
			Map<String, Integer> lotSizeByStockCode, //
			List<Long> tradeEpochDays, //
			long backTestEpochDay, //
			float valuation0) {
		long oneYearAgo = backTestEpochDay - 365l;

		int nTradeDaysInYear = Read.from(tradeEpochDays) //
				.filter(epochDay -> oneYearAgo <= epochDay && epochDay < backTestEpochDay) //
				.size();

		Map<String, MeanReversionStats> meanReversionStatsByStockCode = Read.from2(dataSourceByStockCode) //
				.mapValue(MeanReversionStats::new) //
				.toMap();

		// make sure all time-series are mean-reversions:
		// ensure ADF < 0f: price is not random walk
		// ensure Hurst exponent < .5f: price is weakly mean reverting
		// ensure 0f < variable ratio: statistic is significant
		// ensure 0 < half-life: determine investment period
		return Read.from2(meanReversionStatsByStockCode) //
				.filterValue(mrs -> mrs.adf < 0f //
						&& mrs.hurst < .5f //
						&& 0f < mrs.varianceRatio //
						&& 0f < mrs.movingAvgMeanReversionRatio) //
				.map2((stockCode, mrs) -> stockCode, (stockCode, mrs) -> {
					double price = dataSourceByStockCode.get(stockCode).get(-1).price;
					double lma = mrs.latestMovingAverage();
					double potential = (lma / price - 1d) * mrs.movingAvgMeanReversionRatio;
					double yearReturn = Math.exp(Math.log1p(potential) * nTradeDaysInYear);
					System.out.println(hkex.getCompany(stockCode) //
							+ ", mamrRatio = " + mrs.movingAvgMeanReversionRatio //
							+ ", " + MathUtil.format(price) + " => " + MathUtil.format(lma) //
							+ ", potential = " + MathUtil.format(potential) //
							+ ", yearReturn = " + MathUtil.format(yearReturn));
					return yearReturn;
				}) //
				.filterValue(yearReturn -> riskFreeInterestRate < yearReturn) //
				.sortBy((stockCode, potential) -> -potential) //
				.take(top) //
				.keys() //
				.map2(stockCode -> stockCode, stockCode -> {
					int lotSize = lotSizeByStockCode.get(stockCode);
					float price = dataSourceByStockCode.get(stockCode).get(-1).price;
					return lotSize * (int) Math.round(valuation0 / (top * lotSize * price));
				}) //
				.toMap();
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

		public MeanReversionStats(DataSource dataSource) {
			float[] prices = dataSource.prices;

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
