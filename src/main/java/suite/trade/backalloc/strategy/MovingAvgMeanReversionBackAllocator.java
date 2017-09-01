package suite.trade.backalloc.strategy;

import java.util.Map;

import suite.math.stat.Statistic.LinearRegression;
import suite.math.stat.TimeSeries;
import suite.math.stat.TimeSeries.ReturnsStat;
import suite.streamlet.Read;
import suite.trade.Asset;
import suite.trade.TimeRange;
import suite.trade.Trade_;
import suite.trade.analysis.MovingAverage;
import suite.trade.backalloc.BackAllocator;
import suite.trade.data.DataSource;
import suite.trade.data.DataSource.AlignKeyDataSource;
import suite.trade.data.DataSourceView;
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
	public OnDateTime allocate(AlignKeyDataSource<String> akds, int[] indices) {
		Map<String, DataSource> dsBySymbol = akds.dsByKey.toMap();

		log.sink(dsBySymbol.size() + " assets in data source");
		double dailyRiskFreeInterestRate = Trade_.riskFreeInterestRate(1);

		DataSourceView<String, MeanReversionStat> dsv = DataSourceView //
				.of(tor, 256, akds, (symbol, ds, period) -> new MeanReversionStat(ds, period));

		return index -> {
			Map<String, MeanReversionStat> mrsBySymbol = akds.dsByKey //
					.map2((symbol, ds) -> dsv.get(symbol, index)) //
					.filterValue(mrsReversionStat -> mrsReversionStat != null) //
					.toMap();

			// make sure all time-series are mean-reversions:
			// ensure ADF < 0d: price is not random walk
			// ensure Hurst exponent < .5d: price is weakly mean reverting
			// ensure 0 < half-life: determine investment period
			return Read.from2(mrsBySymbol) //
					.filterValue(mrs -> mrs.adf < 0d //
							&& mrs.hurst < .5d //
							&& mrs.movingAvgMeanReversionRatio() < 0d) //
					.map2((symbol, mrs) -> {
						DataSource ds = dsBySymbol.get(symbol);
						double price = ds.prices[index - 1];

						double lma = mrs.latestMovingAverage();
						float diff = mrs.movingAvgMeanReversion.predict(new float[] { (float) lma, 1f, });
						double dailyReturn = diff / price - dailyRiskFreeInterestRate;

						ReturnsStat returnsStat = ts.returnsStatDaily(ds.prices);
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
		private final double dailyReturn;
		private final double sharpe;
		private final double kelly;

		private PotentialStat(double dailyReturn, double sharpe, double kelly) {
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

	private class MeanReversionStat {
		private final float[] movingAverage;
		private final double adf;
		private final double hurst;
		private final LinearRegression meanReversion;
		private final LinearRegression movingAvgMeanReversion;

		private MeanReversionStat(DataSource ds, TimeRange mrsPeriod) {
			float[] prices = ds.range(mrsPeriod).prices;

			movingAverage = ma.geometricMovingAvg(prices, tor);

			if (tor <= prices.length) {
				adf = ts.adf(prices, tor);
				hurst = ts.hurst(prices, tor);
				meanReversion = ts.meanReversion(prices, 1);
				movingAvgMeanReversion = ts.movingAvgMeanReversion(prices, movingAverage, tor);
			} else {
				meanReversion = movingAvgMeanReversion = null;
				adf = hurst = 0d;
			}
		}

		private float latestMovingAverage() {
			return movingAverage[movingAverage.length - 1];
		}

		private double meanReversionRatio() {
			return meanReversion.coefficients[0];
		}

		private double movingAvgMeanReversionRatio() {
			return movingAvgMeanReversion.coefficients[0];
		}

		private double halfLife() {
			return neglog2 / Math.log1p(meanReversionRatio());
		}

		private double movingAvgHalfLife() {
			return neglog2 / Math.log1p(movingAvgMeanReversionRatio());
		}

		public String toString() {
			return "adf = " + adf //
					+ ", hurst = " + hurst //
					+ ", halfLife = " + halfLife() //
					+ ", movingAvgHalfLife = " + movingAvgHalfLife() //
					+ ", latestMovingAverage = " + latestMovingAverage();
		}
	}

}
