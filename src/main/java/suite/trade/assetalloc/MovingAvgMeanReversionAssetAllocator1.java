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
import suite.math.TimeSeries.ReturnsStat;
import suite.streamlet.Read;
import suite.trade.Asset;
import suite.trade.DatePeriod;
import suite.trade.MovingAverage;
import suite.trade.data.Configuration;
import suite.trade.data.DataSource;
import suite.util.FunUtil.Sink;
import suite.util.To;

public class MovingAvgMeanReversionAssetAllocator1 implements AssetAllocator {

	private int top = 5;
	private int tor = 64;

	private double neglog2 = -Math.log(2d);

	private Matrix mtx = new Matrix();
	private Statistic stat = new Statistic();
	private MovingAverage movingAvg = new MovingAverage();
	private TimeSeries ts = new TimeSeries();

	private Configuration cfg;
	private Sink<String> log;

	public static AssetAllocator of(Configuration cfg, Sink<String> log) {
		return MovingAvgMeanReversionAssetAllocator1.of_(cfg, log);
	}

	public static MovingAvgMeanReversionAssetAllocator1 of_(Configuration cfg, Sink<String> log) {
		return new MovingAvgMeanReversionAssetAllocator1(cfg, log);
	}

	private MovingAvgMeanReversionAssetAllocator1(Configuration cfg, Sink<String> log) {
		this.cfg = cfg;
		this.log = log;
	}

	public List<Pair<String, Double>> allocate( //
			Map<String, DataSource> dataSourceBySymbol, //
			List<LocalDate> tradeDates, //
			LocalDate backTestDate) {
		LocalDate oneYearAgo = backTestDate.minusYears(1);

		int nTradeDaysInYear = Read.from(tradeDates) //
				.filter(tradeDate -> oneYearAgo.compareTo(tradeDate) <= 0 && tradeDate.compareTo(backTestDate) < 0) //
				.size();

		log.sink(dataSourceBySymbol.size() + " assets in data source");

		DatePeriod mrsPeriod = DatePeriod.backTestDaysBefore(backTestDate.minusDays(tor), 256, 32);

		Map<String, MeanReversionStat> meanReversionStatBySymbol = Read.from2(dataSourceBySymbol) //
				.map2((symbol, dataSource) -> symbol, (symbol, dataSource) -> meanReversionStat(symbol, dataSource, mrsPeriod)) //
				.toMap();

		double dailyRiskFreeInterestRate = Math.expm1(stat.logRiskFreeInterestRate / nTradeDaysInYear);

		// make sure all time-series are mean-reversions:
		// ensure ADF < 0d: price is not random walk
		// ensure Hurst exponent < .5d: price is weakly mean reverting
		// ensure 0d < variance ratio: statistic is significant
		// ensure 0 < half-life: determine investment period
		return Read.from2(meanReversionStatBySymbol) //
				.filterValue(mrs -> mrs.adf < 0d //
						&& mrs.hurst < .5d //
						&& 0d < mrs.varianceRatio //
						&& mrs.movingAvgMeanReversionRatio() < 0d) //
				.map2((symbol, mrs) -> symbol, (symbol, mrs) -> {
					DataSource dataSource = dataSourceBySymbol.get(symbol);
					double price = dataSource.last().price;

					double lma = mrs.latestMovingAverage();
					float diff = mrs.movingAvgMeanReversion.predict(new float[] { (float) lma, 1f, });
					double dailyReturn = diff / price - dailyRiskFreeInterestRate;

					ReturnsStat returnsStat = ts.returnsStat(dataSource.prices);
					double sharpe = returnsStat.sharpeRatio();
					double kelly = dailyReturn * price * price / mrs.movingAvgMeanReversion.sse;

					PotentialStat potentialStat = new PotentialStat(dailyReturn, sharpe, kelly);

					log.sink(cfg.queryCompany(symbol) //
							+ ", mrRatio = " + To.string(mrs.meanReversionRatio()) //
							+ ", mamrRatio = " + To.string(mrs.movingAvgMeanReversionRatio()) //
							+ ", sse = " + mrs.movingAvgMeanReversion.sse //
							+ ", " + To.string(price) + " => " + To.string(price + diff) //
							+ ", " + potentialStat);

					return potentialStat;
				}) //
				.filterValue(ps -> 0d < ps.dailyReturn) //
				.filterValue(ps -> 0d < ps.sharpe) //
				.cons(Asset.cashCode, new PotentialStat(stat.riskFreeInterestRate, 1d, 0d)) //
				.mapValue(ps -> ps.potential) //
				.sortBy((symbol, potential) -> -potential) //
				.take(top) //
				.toList();
	}

	private class PotentialStat {
		public final double dailyReturn;
		public final double sharpe;
		public final double potential;

		public PotentialStat(double dailyReturn, double sharpe, double potential) {
			this.dailyReturn = dailyReturn;
			this.sharpe = sharpe;
			this.potential = potential;
		}

		public String toString() {
			return "dailyReturn = " + To.string(dailyReturn) //
					+ ", sharpe = " + To.string(sharpe) //
					+ ", potential = " + To.string(potential);
		}
	}

	private MeanReversionStat meanReversionStat(String symbol, DataSource dataSource, DatePeriod period) {
		Pair<String, DatePeriod> key = Pair.of(symbol, period);
		return memoizeMeanReversionStat.computeIfAbsent(key, p -> new MeanReversionStat(dataSource, period));
	}

	private static Map<Pair<String, DatePeriod>, MeanReversionStat> memoizeMeanReversionStat = new ConcurrentHashMap<>();

	public class MeanReversionStat {
		public final float[] movingAverage;
		public final double adf;
		public final double hurst;
		public final double varianceRatio;
		public final LinearRegression meanReversion;
		public final LinearRegression movingAvgMeanReversion;

		public MeanReversionStat(DataSource dataSource0, DatePeriod mrsPeriod) {
			DataSource dataSource = dataSource0.range(mrsPeriod);
			float[] prices = dataSource.prices;

			movingAverage = movingAvg.movingGeometricAvg(prices, tor);

			if (tor <= prices.length) {
				adf = adf(prices, tor);
				hurst = hurst(prices, tor);
				varianceRatio = varianceRatio(prices, tor);
				meanReversion = meanReversion(prices, 1);
				movingAvgMeanReversion = movingAvgMeanReversion(prices, movingAverage, tor);
			} else {
				meanReversion = movingAvgMeanReversion = null;
				adf = hurst = varianceRatio = 0d;
			}
		}

		public float latestMovingAverage() {
			return movingAverage[movingAverage.length - 1];
		}

		public double meanReversionRatio() {
			return meanReversion.betas[0];
		}

		public double movingAvgMeanReversionRatio() {
			return movingAvgMeanReversion.betas[0];
		}

		public double halfLife() {
			return neglog2 / Math.log1p(meanReversionRatio());
		}

		public double movingAvgHalfLife() {
			return neglog2 / Math.log1p(movingAvgMeanReversionRatio());
		}

		public String toString() {
			return "adf = " + adf //
					+ ", hurst = " + hurst //
					+ ", varianceRatio = " + varianceRatio //
					+ ", halfLife = " + halfLife() //
					+ ", movingAvgHalfLife = " + movingAvgHalfLife() //
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

	private LinearRegression meanReversion(float[] prices, int tor) {
		float[][] deps = To.array(float[].class, prices.length - tor, i -> new float[] { prices[i], 1f, });
		float[] diffs1 = ts.dropDiff(tor, prices);
		return stat.linearRegression(deps, diffs1);
	}

	private LinearRegression movingAvgMeanReversion(float[] prices, float[] movingAvg, int tor) {
		float[] ma = ts.drop(tor, movingAvg);
		float[][] deps = To.array(float[].class, prices.length - tor, i -> new float[] { ma[i], 1f, });
		float[] diffs1 = ts.drop(tor, ts.differences(1, prices));
		return stat.linearRegression(deps, diffs1);
	}

}
