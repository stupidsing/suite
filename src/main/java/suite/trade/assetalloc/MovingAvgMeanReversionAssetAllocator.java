package suite.trade.assetalloc;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import suite.adt.Pair;
import suite.algo.Statistic;
import suite.algo.Statistic.LinearRegression;
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

/**
 * Find some mean-reverting stock, make sure they are below their past moving
 * averages, predict their Sharpe ratios and Kelly criterions, then trade them
 * accordingly.
 *
 * @author ywsing
 */
public class MovingAvgMeanReversionAssetAllocator implements AssetAllocator {

	private int top = 5;
	private int tor = 64;

	private double neglog2 = -Math.log(2d);

	private Statistic stat = new Statistic();
	private MovingAverage movingAvg = new MovingAverage();
	private TimeSeries ts = new TimeSeries();

	private Configuration cfg;
	private Sink<String> log;

	private Map<Pair<String, DatePeriod>, MeanReversionStat> memoizeMrs = new HashMap<>();

	public static AssetAllocator of(Configuration cfg, Sink<String> log) {
		return MovingAvgMeanReversionAssetAllocator.of_(cfg, log);
	}

	public static MovingAvgMeanReversionAssetAllocator of_(Configuration cfg, Sink<String> log) {
		return new MovingAvgMeanReversionAssetAllocator(cfg, log);
	}

	private MovingAvgMeanReversionAssetAllocator(Configuration cfg, Sink<String> log) {
		this.cfg = cfg;
		this.log = log;
	}

	public List<Pair<String, Double>> allocate( //
			Map<String, DataSource> dataSourceBySymbol, //
			List<LocalDate> tradeDates, //
			LocalDate backTestDate) {
		log.sink(dataSourceBySymbol.size() + " assets in data source");

		DatePeriod mrsPeriod = DatePeriod.backTestDaysBefore(backTestDate.minusDays(tor), 256, 32);
		DatePeriod backTestPeriod = DatePeriod.yearsBefore(backTestDate, 1);
		int nTradeDaysInYear = Read.from(tradeDates).filter(backTestPeriod::contains).size();

		Map<String, MeanReversionStat> meanReversionStatBySymbol = Read.from2(dataSourceBySymbol) //
				.map2((symbol, dataSource) -> memoizeMrs.computeIfAbsent( //
						Pair.of(symbol, mrsPeriod), //
						p -> meanReversionStat(symbol, dataSource, mrsPeriod))) //
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
				.map2((symbol, mrs) -> {
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
							+ ", " + To.string(price) + " => " + To.string(price + diff) //
							+ ", " + potentialStat);

					return potentialStat;
				}) //
				.filterValue(ps -> 0d < ps.kelly) //
				.cons(Asset.cashCode, new PotentialStat(stat.riskFreeInterestRate, 1d, 0d)) //
				.mapValue(ps -> ps.kelly) //
				.sortBy((symbol, potential) -> -potential) //
				.take(top) //
				.toList();
	}

	private class PotentialStat {
		public final double dailyReturn;
		public final double sharpe;
		public final double kelly;

		public PotentialStat(double dailyReturn, double sharpe, double kelly) {
			this.dailyReturn = dailyReturn;
			this.sharpe = sharpe;
			this.kelly = kelly;
		}

		public String toString() {
			return "dailyReturn = " + To.string(dailyReturn) //
					+ ", sharpe = " + To.string(sharpe) //
					+ ", kelly = " + To.string(kelly);
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
				adf = ts.adf(prices, tor);
				hurst = ts.hurst(prices, tor);
				varianceRatio = ts.varianceRatio(prices, tor);
				meanReversion = ts.meanReversion(prices, 1);
				movingAvgMeanReversion = ts.movingAvgMeanReversion(prices, movingAverage, tor);
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

}
