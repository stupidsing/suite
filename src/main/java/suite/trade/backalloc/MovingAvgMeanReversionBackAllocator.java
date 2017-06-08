package suite.trade.backalloc;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import suite.adt.pair.Pair;
import suite.math.stat.Statistic.LinearRegression;
import suite.math.stat.TimeSeries;
import suite.math.stat.TimeSeries.ReturnsStat;
import suite.streamlet.Read;
import suite.streamlet.Streamlet2;
import suite.trade.Asset;
import suite.trade.MovingAverage;
import suite.trade.Time;
import suite.trade.TimeRange;
import suite.trade.Trade_;
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
public class MovingAvgMeanReversionBackAllocator implements BackAllocator {

	private int top = 5;
	private int tor = 64;
	private double neglog2 = -Math.log(2d);

	private Sink<String> log;
	private MovingAverage ma = new MovingAverage();
	private TimeSeries ts = new TimeSeries();

	public static BackAllocator of(Sink<String> log) {
		return MovingAvgMeanReversionBackAllocator.of_(log);
	}

	public static MovingAvgMeanReversionBackAllocator of_(Sink<String> log) {
		return new MovingAvgMeanReversionBackAllocator(log);
	}

	private MovingAvgMeanReversionBackAllocator(Sink<String> log) {
		this.log = log;
	}

	@Override
	public OnDateTime allocate(Streamlet2<String, DataSource> dataSourceBySymbol, List<Time> times) {
		log.sink(dataSourceBySymbol.size() + " assets in data source");
		double dailyRiskFreeInterestRate = Trade_.riskFreeInterestRate(1);

		Map<String, Map<TimeRange, MeanReversionStat>> meanReversionStatByPeriodBySymbol = dataSourceBySymbol //
				.map2((symbol, dataSource) -> TimeRange //
						.ofDateTimes(times) //
						.plusDays(-tor) //
						.backTestDaysBefore(256, 32) //
						.map2(mrsPeriod -> meanReversionStat(symbol, dataSource, mrsPeriod)) //
						.toMap()) //
				.toMap();

		return (time, index) -> {
			Map<String, DataSource> dataSources = dataSourceBySymbol.toMap();
			TimeRange mrsPeriod = TimeRange.backTestDaysBefore(time.addDays(-tor), 256, 32);

			Map<String, MeanReversionStat> meanReversionStatBySymbol = dataSourceBySymbol //
					.map2((symbol, dataSource) -> {
						Map<TimeRange, MeanReversionStat> m = meanReversionStatByPeriodBySymbol.get(symbol);
						return m != null ? m.get(mrsPeriod) : null;
					}) //
					.filterValue(mrsReversionStat -> mrsReversionStat != null) //
					.toMap();

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
						DataSource dataSource = dataSources.get(symbol);
						double price = dataSource.prices[index - 1];

						double lma = mrs.latestMovingAverage();
						float diff = mrs.movingAvgMeanReversion.predict(new float[] { (float) lma, 1f, });
						double dailyReturn = diff / price - dailyRiskFreeInterestRate;

						ReturnsStat returnsStat = ts.returnsStat(dataSource.prices);
						double sharpe = returnsStat.sharpeRatio();
						double kelly = dailyReturn * price * price / mrs.movingAvgMeanReversion.sse;

						PotentialStat potentialStat = new PotentialStat(dailyReturn, sharpe, kelly);

						log.sink(symbol //
								+ ", mrRatio = " + To.string(mrs.meanReversionRatio()) //
								+ ", mamrRatio = " + To.string(mrs.movingAvgMeanReversionRatio()) //
								+ ", " + To.string(price) + " => " + To.string(price + diff) //
								+ ", " + potentialStat);

						return potentialStat;
					}) //
					.filterValue(ps -> 0d < ps.kelly) //
					.cons(Asset.cashSymbol, new PotentialStat(Trade_.riskFreeInterestRate, 1d, 0d)) //
					.mapValue(ps -> ps.kelly) //
					.sortBy((symbol, potential) -> -potential) //
					.take(top) //
					.toList();
		};
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

	private MeanReversionStat meanReversionStat(String symbol, DataSource dataSource, TimeRange period) {
		Pair<String, TimeRange> key = Pair.of(symbol, period);
		return memoizeMeanReversionStat.computeIfAbsent(key, p -> new MeanReversionStat(dataSource, period));
	}

	private static Map<Pair<String, TimeRange>, MeanReversionStat> memoizeMeanReversionStat = new ConcurrentHashMap<>();

	public class MeanReversionStat {
		public final float[] movingAverage;
		public final double adf;
		public final double hurst;
		public final double varianceRatio;
		public final LinearRegression meanReversion;
		public final LinearRegression movingAvgMeanReversion;

		public MeanReversionStat(DataSource dataSource, TimeRange mrsPeriod) {
			float[] prices = dataSource.range(mrsPeriod).prices;

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
			return meanReversion.coefficients[0];
		}

		public double movingAvgMeanReversionRatio() {
			return movingAvgMeanReversion.coefficients[0];
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
