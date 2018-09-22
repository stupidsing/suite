package suite.trade.backalloc.strategy;

import static suite.util.Friends.log;
import static suite.util.Friends.log1p;

import suite.math.linalg.Vector;
import suite.math.numeric.Statistic.LinearRegression;
import suite.streamlet.Read;
import suite.trade.Asset;
import suite.trade.TimeRange;
import suite.trade.Trade_;
import suite.trade.analysis.MovingAverage;
import suite.trade.backalloc.BackAllocator;
import suite.trade.data.DataSource;
import suite.trade.data.DataSourceView;
import suite.ts.TimeSeries;
import suite.util.To;

/**
 * Find some mean-reverting stock, make sure they are below their past moving
 * averages, predict their Sharpe ratios and Kelly criterions, then trade them
 * accordingly.
 *
 * @author ywsing
 */
public class MovingAvgMeanReversionBackAllocator {

	private int top = 5;
	private int tor = 64;
	private double neglog2 = -log(2d);

	private MovingAverage ma = new MovingAverage();
	private TimeSeries ts = new TimeSeries();
	private Vector vec = new Vector();

	public BackAllocator backAllocator() {
		return (akds, indices) -> {
			var dsBySymbol = akds.dsByKey.toMap();
			var dailyRiskFreeInterestRate = Trade_.riskFreeInterestRate(1);
			var dsv = DataSourceView.of(tor, 256, akds, (symbol, ds, period) -> new MeanReversionStat(ds, period));

			return index -> {
				var mrsBySymbol = akds.dsByKey //
						.map2((symbol, ds) -> dsv.get(symbol, index)) //
						.filterValue(mrsReversionStat -> mrsReversionStat != null) //
						.toMap();

				// make sure all time-series are mean-reversions:
				// ensure ADF < 0d: price is not random walk
				// ensure Hurst exponent < .5d: price is weakly mean reverting
				// ensure 0 < half-life: determine investment period
				return Read //
						.from2(mrsBySymbol) //
						.filterValue(mrs -> mrs.adf < 0d //
								&& mrs.hurst < .5d //
								&& mrs.movingAvgMeanReversionRatio() < 0d) //
						.map2((symbol, mrs) -> {
							var ds = dsBySymbol.get(symbol);
							var price = ds.prices[index - 1];

							var lma = mrs.latestMovingAverage();
							var diff = mrs.movingAvgMeanReversion.predict(vec.of(lma, 1d));
							var dailyReturn = diff / price - dailyRiskFreeInterestRate;

							var returnsStat = ts.returnsStatDaily(ds.prices);
							var sharpe = returnsStat.sharpeRatio();
							var kelly = dailyReturn * price * price / mrs.movingAvgMeanReversion.sse;
							return new PotentialStat(dailyReturn, sharpe, kelly);
						}) //
						.filterValue(ps -> 0d < ps.kelly) //
						.cons(Asset.cashSymbol, new PotentialStat(Trade_.riskFreeInterestRate, 1d, 0d)) //
						.mapValue(ps -> ps.kelly) //
						.sortBy((symbol, potential) -> -potential) //
						.take(top) //
						.toList();
			};
		};
	}

	private class PotentialStat {
		private double dailyReturn;
		private double sharpe;
		private double kelly;

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
		private float[] movingAverage;
		private double adf;
		private double hurst;
		private LinearRegression meanReversion;
		private LinearRegression movingAvgMeanReversion;

		private MeanReversionStat(DataSource ds, TimeRange mrsPeriod) {
			var prices = ds.range(mrsPeriod).prices;

			movingAverage = ma.geometricMovingAvg(prices, tor);

			if (tor <= prices.length) {
				adf = ts.adf(prices, tor);
				hurst = ts.hurst(prices, tor);
				meanReversion = ts.meanReversion(prices, 1);
				movingAvgMeanReversion = ts.movingAvgMeanReversion(prices, movingAverage, tor);
			} else {
				adf = hurst = 0d;
				meanReversion = movingAvgMeanReversion = null;
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
			return neglog2 / log1p(meanReversionRatio());
		}

		private double movingAvgHalfLife() {
			return neglog2 / log1p(movingAvgMeanReversionRatio());
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
