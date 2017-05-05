package suite.trade;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import suite.algo.Statistic;
import suite.algo.Statistic.LinearRegression;
import suite.math.Matrix;
import suite.math.TimeSeries;
import suite.os.LogUtil;
import suite.streamlet.Read;
import suite.streamlet.Streamlet;
import suite.trade.DataSource.Datum;
import suite.util.FormatUtil;
import suite.util.FunUtil.Fun;
import suite.util.FunUtil.Sink;
import suite.util.To;
import suite.util.Util;

public class Portfolio {

	private float riskFreeInterestRate = 1.05f;
	private int top = 5;
	private int tor = 64;
	private int historyWindow = 1024;

	private double neglog2 = -Math.log(2d);

	private Matrix mtx = new Matrix();
	private Statistic stat = new Statistic();
	private MovingAverage movingAvg = new MovingAverage();
	private TimeSeries ts = new TimeSeries();

	private Hkex hkex = new Hkex();
	private HkexFactBook hkexFactBook = new HkexFactBook();
	private Yahoo yahoo = new Yahoo();

	private Sink<String> log;

	public Portfolio() {
		log = System.out::println;
	}

	public Portfolio(Sink<String> log) {
		this.log = log;
	}

	public Simulate simulateLatest(float fund0) {
		return new Simulate(fund0, LocalDate.now(), dates -> Arrays.asList(Util.last(dates)));
	}

	public Simulate simulateFromTo(float fund0, LocalDate from, LocalDate to) {
		Fun<List<LocalDate>, List<LocalDate>> datesPred = dates -> Read.from(dates) //
				.filter(date -> true //
						&& from.compareTo(date) <= 0 //
						&& date.compareTo(to) < 0) //
				.toList();
		return new Simulate(fund0, from, datesPred);
	}

	public class Simulate {
		public final Account account;
		public final float[] valuations;

		private Simulate(float fund0, LocalDate from, Fun<List<LocalDate>, List<LocalDate>> datesPred) {
			account = Account.fromCash(fund0);

			LocalDate historyFromDate = from.minusYears(1);
			Map<String, DataSource> dataSourceByStockCode = new HashMap<>();
			Streamlet<Asset> assets = hkexFactBook.queryLeadingCompaniesByMarketCap(from.getYear() - 1);
			// hkex.getCompanies();

			Map<String, Integer> lotSizeByStockCode = hkex.queryLotSizeByStockCode(assets);

			for (Asset asset : assets) {
				String stockCode = asset.code;
				if (lotSizeByStockCode.containsKey(stockCode))
					try {
						DataSource dataSource = yahoo.dataSourceWithLatestQuote(stockCode).limitAfter(historyFromDate);
						dataSource.validate();
						dataSourceByStockCode.put(stockCode, dataSource);
					} catch (Exception ex) {
						LogUtil.warn(ex.getMessage() + " in " + asset);
					}
			}

			List<LocalDate> tradeDates = Read.from2(dataSourceByStockCode) //
					.concatMap((stockCode, dataSource) -> Read.from(dataSource.dates)) //
					.distinct() //
					.map(tradeDate -> FormatUtil.date(tradeDate)) //
					.sort(Util::compare) //
					.toList();

			List<LocalDate> dates = datesPred.apply(tradeDates);
			valuations = new float[dates.size()];

			for (int i = 0; i < dates.size(); i++) {
				LocalDate date = dates.get(i);
				DatePeriod historyWindowPeriod = DatePeriod.of(date.minusDays(historyWindow), date);

				Map<String, DataSource> backTestDataSourceByStockCode = Read.from2(dataSourceByStockCode) //
						.mapValue(dataSource -> dataSource.limit(historyWindowPeriod)) //
						.filterValue(dataSource -> 128 <= dataSource.dates.length) //
						.toMap();

				Map<String, Float> latestPriceByStockCode = Read.from2(backTestDataSourceByStockCode) //
						.mapValue(dataSource -> dataSource.last().price) //
						.toMap();

				Map<String, Integer> portfolio = formPortfolio( //
						backTestDataSourceByStockCode, //
						lotSizeByStockCode, //
						tradeDates, //
						date, //
						fund0);

				String actions = account.portfolio(portfolio, latestPriceByStockCode);
				account.validate();

				double valuation1 = valuations[i] = account.valuation(latestPriceByStockCode);

				log.sink(FormatUtil.formatDate(date) //
						+ ", valuation = " + valuation1 //
						+ ", portfolio = " + TradeUtil.format(portfolio) //
						+ ", actions = " + actions);
			}

			LocalDate dateStart = Util.first(dates);
			LocalDate dateEnd = Util.last(dates);
			double v0 = valuations[0];
			double vx = valuations[valuations.length - 1];

			double nYears = (dateEnd.toEpochDay() - dateStart.toEpochDay()) / 365d;
			double annualReturn = Math.exp(Math.log(vx / v0) / nYears);
			double sharpe = ts.sharpeRatio(valuations, nYears);

			log.sink("annual return = " + To.string(annualReturn) + ", sharpe = " + To.string(sharpe));
		}
	}

	private Map<String, Integer> formPortfolio( //
			Map<String, DataSource> dataSourceByStockCode, //
			Map<String, Integer> lotSizeByStockCode, //
			List<LocalDate> tradeDates, //
			LocalDate backTestDate, //
			float valuation0) {
		LocalDate oneYearAgo = backTestDate.minusYears(1);

		int nTradeDaysInYear = Read.from(tradeDates) //
				.filter(tradeDate -> oneYearAgo.compareTo(tradeDate) <= 0 && tradeDate.compareTo(backTestDate) < 0) //
				.size();

		log.sink(dataSourceByStockCode.size() + " assets in data source");

		Map<String, MeanReversionStats> meanReversionStatsByStockCode = Read.from2(dataSourceByStockCode) //
				.mapValue(MeanReversionStats::new) //
				.toMap();

		// make sure all time-series are mean-reversions:
		// ensure ADF < 0f: price is not random walk
		// ensure Hurst exponent < .5f: price is weakly mean reverting
		// ensure 0f < variance ratio: statistic is significant
		// ensure 0 < half-life: determine investment period
		return Read.from2(meanReversionStatsByStockCode) //
				.filterValue(mrs -> mrs.adf < 0f //
						&& mrs.hurst < .5f //
						&& 0f < mrs.varianceRatio //
						&& 0f < mrs.movingAvgMeanReversionRatio) //
				.map2((stockCode, mrs) -> stockCode, (stockCode, mrs) -> {
					DataSource dataSource = dataSourceByStockCode.get(stockCode);
					Datum first = dataSource.first();
					Datum last = dataSource.last();
					long ed0 = FormatUtil.date(first.date).toEpochDay();
					long edx = FormatUtil.date(last.date).toEpochDay();
					double price = last.price;

					double lma = mrs.latestMovingAverage();
					double potential = (lma / price - 1d) * mrs.movingAvgMeanReversionRatio;
					double annualReturn = Math.exp(Math.log1p(potential) * nTradeDaysInYear);
					double sharpe = ts.sharpeRatio(dataSource.prices, (edx - ed0) / 365d);
					log.sink(hkex.getCompany(stockCode) //
							+ ", mamrRatio = " + To.string(mrs.movingAvgMeanReversionRatio) //
							+ ", " + To.string(price) + " => " + To.string(lma) //
							+ ", annualReturn = " + To.string(annualReturn) //
							+ ", sharpe = " + To.string(sharpe));
					return annualReturn;
				}) //
				.filterValue(annualReturn -> riskFreeInterestRate < annualReturn) //
				.sortBy((stockCode, potential) -> -potential) //
				.take(top) //
				.keys() //
				.map2(stockCode -> stockCode, stockCode -> {
					int lotSize = lotSizeByStockCode.get(stockCode);
					float price = dataSourceByStockCode.get(stockCode).last().price;
					return lotSize * (int) Math.round(valuation0 / (top * lotSize * price));
				}) //
				.toMap();
	}

	public class MeanReversionStats {
		public final float[] movingAverage;
		public final double adf;
		public final double hurst;
		public final double varianceRatio;
		public final double meanReversionRatio;
		public final double movingAvgMeanReversionRatio;
		public final double halfLife;
		public final double movingAvgHalfLife;

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
	private double adf(DataSource dataSource, int tor) {
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
		return lambda / lr.standardError;
	}

	private double hurst(DataSource dataSource, int tor) {
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
		return beta0 / 2d;
	}

	private double varianceRatio(DataSource dataSource, int tor) {
		float[] prices = dataSource.prices;
		float[] logs = To.floatArray(prices, price -> (float) Math.log(price));
		float[] diffsTor = ts.dropDiff(tor, logs);
		float[] diffs1 = ts.dropDiff(1, logs);
		return stat.variance(diffsTor) / (tor * stat.variance(diffs1));
	}

	private double meanReversionRatio(DataSource dataSource, int tor) {
		float[] prices = dataSource.prices;
		float[][] deps = To.array(float[].class, prices.length - tor, i -> new float[] { prices[i], 1f, });
		float[] diffs1 = ts.dropDiff(tor, prices);
		LinearRegression lr = stat.linearRegression(deps, diffs1);
		return lr.betas[0];
	}

	private double movingAvgMeanReversionRatio(DataSource dataSource, float[] movingAvg, int tor) {
		float[] prices = dataSource.prices;
		float[] ma = ts.drop(tor, movingAvg);
		float[][] deps = To.array(float[].class, prices.length - tor, i -> new float[] { ma[i], 1f, });
		float[] diffs1 = ts.dropDiff(tor, prices);
		LinearRegression lr = stat.linearRegression(deps, diffs1);
		return lr.betas[0];
	}

}
