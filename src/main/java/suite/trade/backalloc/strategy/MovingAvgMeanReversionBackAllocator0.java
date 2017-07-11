package suite.trade.backalloc.strategy;

import java.util.Map;

import suite.math.stat.Quant;
import suite.math.stat.Statistic.LinearRegression;
import suite.math.stat.TimeSeries;
import suite.math.stat.TimeSeries.ReturnsStat;
import suite.streamlet.Read;
import suite.trade.Asset;
import suite.trade.MovingAverage;
import suite.trade.TimeRange;
import suite.trade.Trade_;
import suite.trade.backalloc.BackAllocator;
import suite.trade.data.DataSource;
import suite.trade.data.DataSource.AlignKeyDataSource;
import suite.trade.data.DataSourceView;
import suite.util.FunUtil.Sink;
import suite.util.To;

/**
 * Find some mean-reverting stock, make sure they are below their past moving
 * averages, find their Sharpe ratio and Kelly criterion from past returns, then
 * trade them accordingly.
 *
 * @author ywsing
 */
public class MovingAvgMeanReversionBackAllocator0 implements BackAllocator {

	private int top = 5;
	private int tor = 64;
	private double neglog2 = -Math.log(2d);

	private Sink<String> log;
	private MovingAverage ma = new MovingAverage();
	private TimeSeries ts = new TimeSeries();

	public static BackAllocator of(Sink<String> log) {
		return MovingAvgMeanReversionBackAllocator0.of_(log).frequency(3).reallocate();
	}

	public static MovingAvgMeanReversionBackAllocator0 of_(Sink<String> log) {
		return new MovingAvgMeanReversionBackAllocator0(log);
	}

	private MovingAvgMeanReversionBackAllocator0(Sink<String> log) {
		this.log = log;
	}

	@Override
	public OnDateTime allocate(AlignKeyDataSource<String> akds, int[] indices) {
		Map<String, DataSource> dsBySymbol = akds.dsByKey.toMap();

		log.sink(dsBySymbol.size() + " assets in data source");
		double dailyRiskFreeInterestRate = Trade_.riskFreeInterestRate(1);

		DataSourceView<String, MeanReversionStat> dsv = DataSourceView //
				.of(tor, akds, indices, (symbol, ds, period) -> new MeanReversionStat(ds, period));

		return (time, index) -> {
			Map<String, MeanReversionStat> mrsBySymbol = akds.dsByKey //
					.map2((symbol, ds) -> dsv.get(symbol, time)) //
					.filterValue(mrsReversionStat -> mrsReversionStat != null) //
					.toMap();

			// make sure all time-series are mean-reversions:
			// ensure ADF < 0d: price is not random walk
			// ensure Hurst exponent < .5d: price is weakly mean reverting
			// ensure 0d < variance ratio: statistic is significant
			return Read.from2(mrsBySymbol) //
					.filterValue(mrs -> mrs.adf < 0d //
							&& mrs.hurst < .5d //
							&& 0d < mrs.varianceRatio) //
					.map2((symbol, mrs) -> {
						DataSource ds = dsBySymbol.get(symbol);
						double price = ds.prices[index - 1];

						double lma = mrs.latestMovingAverage();
						double mamrRatio = mrs.movingAvgMeanReversionRatio();
						double dailyReturn = Quant.return_(price, lma) * mamrRatio - dailyRiskFreeInterestRate;
						ReturnsStat returnsStat = ts.returnsStatDaily(ds.prices);
						double sharpe = returnsStat.sharpeRatio();
						double kelly = returnsStat.kellyCriterion();

						PotentialStat potentialStat = new PotentialStat(dailyReturn, sharpe, kelly);

						log.sink(symbol //
								+ ", mrRatio = " + To.string(mrs.meanReversionRatio()) //
								+ ", mamrRatio = " + To.string(mamrRatio) //
								+ ", " + To.string(price) + " => " + To.string(lma) //
								+ ", " + potentialStat);

						return potentialStat;
					}) //
					.filterValue(ps -> 0d < ps.dailyReturn) //
					.filterValue(ps -> 0d < ps.sharpe) //
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

	public class MeanReversionStat {
		public final float[] movingAverage;
		public final double adf;
		public final double hurst;
		public final double varianceRatio;
		public final LinearRegression meanReversion;
		public final LinearRegression movingAvgMeanReversion;

		public MeanReversionStat(DataSource ds, TimeRange mrsPeriod) {
			float[] prices = ds.range(mrsPeriod).prices;

			movingAverage = ma.movingGeometricAvg(prices, tor);

			if (tor * 2 <= prices.length) {
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
