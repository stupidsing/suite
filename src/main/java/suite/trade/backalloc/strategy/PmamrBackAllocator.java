package suite.trade.backalloc.strategy;

import static suite.util.Friends.log;
import static suite.util.Friends.log1p;

import suite.math.numeric.Statistic.LinearRegression;
import suite.streamlet.Read;
import suite.trade.Instrument;
import suite.trade.TimeRange;
import suite.trade.Trade_;
import suite.trade.analysis.MovingAverage;
import suite.trade.backalloc.BackAllocator;
import suite.trade.data.DataSource;
import suite.trade.data.DataSourceView;
import suite.ts.Quant;
import suite.ts.TimeSeries;
import suite.util.To;

/**
 * Find some mean-reverting stock, make sure they are below their past moving
 * averages, find their Sharpe ratio and Kelly criterion from past returns, then
 * trade them accordingly.
 *
 * @author ywsing
 */
public class PmamrBackAllocator {

	private int top = 5;
	private int tor = 64;
	private double neglog2 = -log(2d);

	private MovingAverage ma = new MovingAverage();
	private TimeSeries ts = new TimeSeries();

	public BackAllocator backAllocator() {
		BackAllocator ba = (akds, indices) -> {
			var dsBySymbol = akds.dsByKey.toMap();

			var dsv = DataSourceView.of(tor, 256, akds, (symbol, ds, period) -> new MeanReversionStat(ds, period));

			return index -> {
				// Time time = Time.ofEpochSec(akds.ts[index - 1]);

				var mrsBySymbol = akds.dsByKey //
						.map2((symbol, ds) -> dsv.get(symbol, index)) //
						.filterValue(mrsReversionStat -> mrsReversionStat != null) //
						.toMap();

				// make sure all time-series are mean-reversions:
				// ensure ADF < 0d: price is not random walk
				// ensure Hurst exponent < .5d: price is weakly mean reverting
				return Read //
						.from2(mrsBySymbol) //
						.filterValue(mrs -> mrs.adf < 0d && mrs.hurst < .5d) //
						.map2((symbol, mrs) -> {
							var ds = dsBySymbol.get(symbol);
							var prices = ds.prices;
							var price = prices[index - 1];

							var lma = mrs.latestMovingAverage();
							var mamrRatio = mrs.movingAvgMeanReversionRatio();
							var dailyReturn = Quant.return_(lma, price) * mamrRatio;
							var returnsStat = ts.returnsStatDaily(prices);
							var sharpe = returnsStat.sharpeRatio();
							var kelly = returnsStat.kellyCriterion();
							return new PotentialStat(dailyReturn, sharpe, kelly);
						}) //
						.filterValue(ps -> ps.dailyReturn < 0d) //
						.filterValue(ps -> 0d < ps.sharpe) //
						.cons(Instrument.cashSymbol, new PotentialStat(Trade_.riskFreeInterestRate, 1d, 0d)) //
						.mapValue(ps -> ps.kelly) //
						.sortBy((symbol, potential) -> -potential) //
						.take(top) //
						.toList();
			};
		};

		return ba.reallocate();
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
		private LinearRegression movingAvgMeanReversion;

		private MeanReversionStat(DataSource ds, TimeRange mrsPeriod) {
			var prices = ds.range(mrsPeriod).prices;
			movingAverage = ma.geometricMovingAvg(prices, tor);

			if (tor * 2 <= prices.length) {
				adf = ts.adf(prices, tor);
				hurst = ts.hurst(prices, tor);
				movingAvgMeanReversion = ts.movingAvgMeanReversion(prices, movingAverage, tor);
			} else {
				adf = hurst = 0d;
				movingAvgMeanReversion = null;
			}
		}

		private float latestMovingAverage() {
			return movingAverage[movingAverage.length - 1];
		}

		private double movingAvgMeanReversionRatio() {
			return movingAvgMeanReversion.coefficients[0];
		}

		private double movingAvgHalfLife() {
			return neglog2 / log1p(movingAvgMeanReversionRatio());
		}

		public String toString() {
			return "adf = " + adf //
					+ ", hurst = " + hurst //
					+ ", movingAvgHalfLife = " + movingAvgHalfLife() //
					+ ", latestMovingAverage = " + latestMovingAverage();
		}
	}

}
