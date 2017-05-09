package suite.trade.assetalloc;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import suite.adt.Pair;
import suite.algo.Statistic;
import suite.algo.Statistic.LinearRegression;
import suite.math.Matrix;
import suite.math.TimeSeries;
import suite.streamlet.Read;
import suite.trade.Asset;
import suite.trade.DatePeriod;
import suite.trade.MovingAverage;
import suite.trade.data.DataSource;
import suite.trade.data.Hkex;
import suite.util.FunUtil.Sink;
import suite.util.To;

public class MovingAvgMeanReversionAssetAllocator implements AssetAllocator {

	private int top = 5;
	private int tor = 64;
	private int tradeFrequency = 3;

	private double neglog2 = -Math.log(2d);

	private Matrix mtx = new Matrix();
	private Statistic stat = new Statistic();
	private MovingAverage movingAvg = new MovingAverage();
	private TimeSeries ts = new TimeSeries();

	private Hkex hkex = new Hkex();

	private Sink<String> log;

	public MovingAvgMeanReversionAssetAllocator(Sink<String> log) {
		this.log = log;
	}

	public List<Pair<String, Double>> allocate( //
			Map<String, DataSource> dataSourceByStockCode, //
			List<LocalDate> tradeDates, //
			LocalDate backTestDate) {
		if (backTestDate.toEpochDay() % tradeFrequency == 0)
			return allocate0(dataSourceByStockCode, tradeDates, backTestDate);
		else
			return null;
	}

	public List<Pair<String, Double>> allocate0( //
			Map<String, DataSource> dataSourceByStockCode, //
			List<LocalDate> tradeDates, //
			LocalDate backTestDate) {
		LocalDate oneYearAgo = backTestDate.minusYears(1);

		int nTradeDaysInYear = Read.from(tradeDates) //
				.filter(tradeDate -> oneYearAgo.compareTo(tradeDate) <= 0 && tradeDate.compareTo(backTestDate) < 0) //
				.size();

		log.sink(dataSourceByStockCode.size() + " assets in data source");

		DatePeriod mrsPeriod = DatePeriod.backTestDaysBefore(backTestDate.minusDays(tor), 256, 32);

		Map<String, MeanReversionStats> meanReversionStatsByStockCode = Read.from2(dataSourceByStockCode) //
				.map2((stockCode, dataSource) -> stockCode,
						(stockCode, dataSource) -> meanReversionStats(stockCode, dataSource, mrsPeriod)) //
				.toMap();

		// make sure all time-series are mean-reversions:
		// ensure ADF < 0d: price is not random walk
		// ensure Hurst exponent < .5d: price is weakly mean reverting
		// ensure 0d < variance ratio: statistic is significant
		// ensure 0 < half-life: determine investment period
		return Read.from2(meanReversionStatsByStockCode) //
				.filterValue(mrs -> mrs.adf < 0f //
						&& mrs.hurst < .5f //
						&& 0f < mrs.varianceRatio //
						&& 0f < mrs.movingAvgMeanReversionRatio) //
				.map2((stockCode, mrs) -> stockCode, (stockCode, mrs) -> {
					DataSource dataSource = dataSourceByStockCode.get(stockCode);
					double price = dataSource.last().price;

					double lma = mrs.latestMovingAverage();
					double dailyReturn = (lma / price - 1d) * mrs.movingAvgMeanReversionRatio;
					double annualReturn = Math.expm1(Math.log1p(dailyReturn) * nTradeDaysInYear);
					double sharpe = ts.sharpeRatio(dataSource.prices, dataSource.nYears());
					log.sink(hkex.getCompany(stockCode) //
							+ ", mamrRatio = " + To.string(mrs.movingAvgMeanReversionRatio) //
							+ ", " + To.string(price) + " => " + To.string(lma) //
							+ ", annualReturn = " + To.string(annualReturn) //
							+ ", sharpe = " + To.string(sharpe));

					return new PotentialStats(annualReturn, sharpe);
				}) //
				.filterValue(ps -> stat.riskFreeInterestRate < ps.annualReturn) //
				.filterValue(ps -> 0d < ps.sharpe) //
				.cons(Asset.cash.code, new PotentialStats(stat.riskFreeInterestRate, 3d)) //
				.mapValue(ps -> ps.potential) //
				.sortBy((stockCode, potential) -> -potential) //
				.take(top) //
				.toList();
	}

	private class PotentialStats {
		public final double annualReturn;
		public final double sharpe;
		public final double potential;

		public PotentialStats(double annualReturn, double sharpe) {
			this.annualReturn = annualReturn;
			this.sharpe = sharpe;
			if (Boolean.FALSE) // Kelly's
				this.potential = annualReturn * sharpe;
			else // even allocation
				this.potential = 1d;
		}
	}

	private MeanReversionStats meanReversionStats(String stockCode, DataSource dataSource, DatePeriod period) {
		Map<Pair<String, DatePeriod>, MeanReversionStats> memoizeMeanReversionStats = new ConcurrentHashMap<>();
		Pair<String, DatePeriod> key = Pair.of(stockCode, period);
		return memoizeMeanReversionStats.computeIfAbsent(key, p -> new MeanReversionStats(dataSource, period));
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

		public MeanReversionStats(DataSource dataSource0, DatePeriod mrsPeriod) {
			DataSource dataSource = dataSource0.range(mrsPeriod);
			float[] prices = dataSource.prices;

			movingAverage = movingAvg.movingGeometricAvg(prices, tor);

			if (tor <= prices.length) {
				adf = adf(prices, tor);
				hurst = hurst(prices, tor);
				varianceRatio = varianceRatio(prices, tor);
				meanReversionRatio = meanReversionRatio(prices, 1);
				movingAvgMeanReversionRatio = movingAvgMeanReversionRatio(prices, movingAverage, tor);
			} else
				adf = hurst = varianceRatio = meanReversionRatio = movingAvgMeanReversionRatio = 0d;

			halfLife = neglog2 / Math.log1p(meanReversionRatio);
			movingAvgHalfLife = neglog2 / Math.log1p(movingAvgMeanReversionRatio);
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
	private double adf(float[] prices, int tor) {
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

	private double hurst(float[] prices, int tor) {
		float[] logPrices = To.arrayOfFloats(prices, price -> (float) Math.log(price));
		int[] tors = To.arrayOfInts(tor, t -> t + 1);
		float[] logVrs = To.arrayOfFloats(tor, t -> {
			float[] diffs = ts.dropDiff(tors[t], logPrices);
			float[] diffs2 = To.arrayOfFloats(diffs, diff -> diff * diff);
			return (float) Math.log(stat.variance(diffs2));
		});
		float[][] deps = To.array(float[].class, logVrs.length, i -> new float[] { logVrs[i], 1f, });
		float[] n = To.arrayOfFloats(logVrs.length, i -> (float) Math.log(tors[i]));
		LinearRegression lr = stat.linearRegression(deps, n);
		float beta0 = lr.betas[0];
		return beta0 / 2d;
	}

	private double varianceRatio(float[] prices, int tor) {
		float[] logs = To.arrayOfFloats(prices, price -> (float) Math.log(price));
		float[] diffsTor = ts.dropDiff(tor, logs);
		float[] diffs1 = ts.dropDiff(1, logs);
		return stat.variance(diffsTor) / (tor * stat.variance(diffs1));
	}

	private double meanReversionRatio(float[] prices, int tor) {
		float[][] deps = To.array(float[].class, prices.length - tor, i -> new float[] { prices[i], 1f, });
		float[] diffs1 = ts.dropDiff(tor, prices);
		LinearRegression lr = stat.linearRegression(deps, diffs1);
		return lr.betas[0];
	}

	private double movingAvgMeanReversionRatio(float[] prices, float[] movingAvg, int tor) {
		float[] ma = ts.drop(tor, movingAvg);
		float[][] deps = To.array(float[].class, prices.length - tor, i -> new float[] { ma[i], 1f, });
		float[] diffs1 = ts.dropDiff(tor, prices);
		LinearRegression lr = stat.linearRegression(deps, diffs1);
		return lr.betas[0];
	}

}
