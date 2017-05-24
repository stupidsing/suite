package suite.trade.assetalloc;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import suite.adt.pair.Pair;
import suite.math.stat.Statistic.LinearRegression;
import suite.math.stat.TimeSeries;
import suite.math.stat.TimeSeries.ReturnsStat;
import suite.streamlet.Read;
import suite.trade.Asset;
import suite.trade.DatePeriod;
import suite.trade.MovingAverage;
import suite.trade.Trade_;
import suite.trade.data.Configuration;
import suite.trade.data.DataSource;
import suite.util.FunUtil.Sink;
import suite.util.To;

/**
 * Find some mean-reverting stock, make sure they are below their past moving
 * averages, find their Sharpe ratio and Kelly criterion from past returns, then
 * trade them accordingly.
 *
 * @author ywsing
 */
public class MovingAvgMeanReversionAssetAllocator0 implements AssetAllocator {

	private int top = 5;
	private int tor = 64;

	private double neglog2 = -Math.log(2d);
	private MovingAverage ma = new MovingAverage();
	private TimeSeries ts = new TimeSeries();

	private Configuration cfg;
	private Sink<String> log;

	private Map<Pair<String, DatePeriod>, MeanReversionStat> memoizeMrs = new HashMap<>();

	public static AssetAllocator of(Configuration cfg, Sink<String> log) {
		return AssetAllocator_.reallocate( //
				AssetAllocator_.byTradeFrequency( //
						3, MovingAvgMeanReversionAssetAllocator0.of_(cfg, log)));
	}

	public static MovingAvgMeanReversionAssetAllocator0 of_(Configuration cfg, Sink<String> log) {
		return new MovingAvgMeanReversionAssetAllocator0(cfg, log);
	}

	private MovingAvgMeanReversionAssetAllocator0(Configuration cfg, Sink<String> log) {
		this.cfg = cfg;
		this.log = log;
	}

	@Override
	public List<Pair<String, Double>> allocate(Map<String, DataSource> dataSourceBySymbol, LocalDate backTestDate, int index) {
		log.sink(dataSourceBySymbol.size() + " assets in data source");

		DatePeriod mrsPeriod = DatePeriod.backTestDaysBefore(backTestDate.minusDays(tor), 256, 32);

		Map<String, MeanReversionStat> meanReversionStatBySymbol = Read.from2(dataSourceBySymbol) //
				.map2((symbol, dataSource) -> memoizeMrs.computeIfAbsent( //
						Pair.of(symbol, mrsPeriod), //
						p -> meanReversionStat(symbol, dataSource, mrsPeriod))) //
				.toMap();

		double dailyRiskFreeInterestRate = Trade_.riskFreeInterestRate(1);

		// make sure all time-series are mean-reversions:
		// ensure ADF < 0d: price is not random walk
		// ensure Hurst exponent < .5d: price is weakly mean reverting
		// ensure 0d < variance ratio: statistic is significant
		return Read.from2(meanReversionStatBySymbol) //
				.filterValue(mrs -> mrs.adf < 0d //
						&& mrs.hurst < .5d //
						&& 0d < mrs.varianceRatio) //
				.map2((symbol, mrs) -> {
					DataSource dataSource = dataSourceBySymbol.get(symbol);
					double price = dataSource.last().price;

					double lma = mrs.latestMovingAverage();
					double mamrRatio = mrs.movingAvgMeanReversionRatio();
					double dailyReturn = (lma / price - 1d) * mamrRatio - dailyRiskFreeInterestRate;
					ReturnsStat returnsStat = ts.returnsStat(dataSource.prices);
					double sharpe = returnsStat.sharpeRatio();
					double kelly = returnsStat.kellyCriterion();

					PotentialStat potentialStat = new PotentialStat(dailyReturn, sharpe, kelly);

					log.sink(cfg.queryCompany(symbol) //
							+ ", mrRatio = " + To.string(mrs.meanReversionRatio()) //
							+ ", mamrRatio = " + To.string(mamrRatio) //
							+ ", " + To.string(price) + " => " + To.string(lma) //
							+ ", " + potentialStat);

					return potentialStat;
				}) //
				.filterValue(ps -> 0d < ps.dailyReturn) //
				.filterValue(ps -> 0d < ps.sharpe) //
				.cons(Asset.cashCode, new PotentialStat(Trade_.riskFreeInterestRate, 1d, 0d)) //
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

			movingAverage = ma.movingGeometricAvg(prices, tor);

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
