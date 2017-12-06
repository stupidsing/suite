package suite.trade.backalloc.strategy;

import java.util.Map;

import suite.math.stat.Quant;
import suite.math.stat.Statistic.LinearRegression;
import suite.math.stat.TimeSeries;
import suite.math.stat.TimeSeries.ReturnsStat;
import suite.streamlet.Read;
import suite.trade.Asset;
import suite.trade.Time;
import suite.trade.TimeRange;
import suite.trade.Trade_;
import suite.trade.analysis.MovingAverage;
import suite.trade.backalloc.BackAllocator;
import suite.trade.data.DataSource;
import suite.trade.data.DataSource.AlignKeyDataSource;
import suite.trade.data.DataSourceView;
import suite.util.To;

/**
 * Find some mean-reverting stock, make sure they are below their past moving
 * averages, find their Sharpe ratio and Kelly criterion from past returns, then
 * trade them accordingly.
 *
 * @author ywsing
 */
public class PmamrBackAllocator implements BackAllocator {

	private int top = 5;
	private int tor = 64;
	private double neglog2 = -Math.log(2d);

	private MovingAverage ma = new MovingAverage();
	private TimeSeries ts = new TimeSeries();

	public static BackAllocator of() {
		return PmamrBackAllocator.of_().reallocate();
	}

	public static PmamrBackAllocator of_() {
		return new PmamrBackAllocator();
	}

	private PmamrBackAllocator() {
	}

	@Override
	public OnDateTime allocate(AlignKeyDataSource<String> akds, int[] indices) {
		Map<String, DataSource> dsBySymbol = akds.dsByKey.toMap();

		DataSourceView<String, MeanReversionStat> dsv = DataSourceView //
				.of(tor, 256, akds, (symbol, ds, period) -> new MeanReversionStat(ds, period));

		return index -> {
			Time time = Time.ofEpochSec(akds.ts[index - 1]);

			Map<String, MeanReversionStat> mrsBySymbol = akds.dsByKey //
					.map2((symbol, ds) -> dsv.get(symbol, index)) //
					.filterValue(mrsReversionStat -> mrsReversionStat != null) //
					.toMap();

			// make sure all time-series are mean-reversions:
			// ensure ADF < 0d: price is not random walk
			// ensure Hurst exponent < .5d: price is weakly mean reverting
			return Read.from2(mrsBySymbol) //
					.filterValue(mrs -> mrs.adf < 0d //
							&& mrs.hurst < .5d) //
					.map2((symbol, mrs) -> {
						DataSource ds = dsBySymbol.get(symbol);
						double price = ds.prices[index - 1];

						double lma = mrs.latestMovingAverage();
						double mamrRatio = mrs.movingAvgMeanReversionRatio();
						double dailyReturn = Quant.return_(lma, price) * mamrRatio;
						ReturnsStat returnsStat = ts.returnsStatDaily(ds.prices);
						double sharpe = returnsStat.sharpeRatio();
						double kelly = returnsStat.kellyCriterion();
						return new PotentialStat(dailyReturn, sharpe, kelly);
					}) //
					.filterValue(ps -> ps.dailyReturn < 0d) //
					.filterValue(ps -> 0d < ps.sharpe) //
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
		private final LinearRegression movingAvgMeanReversion;

		private MeanReversionStat(DataSource ds, TimeRange mrsPeriod) {
			float[] prices = ds.range(mrsPeriod).prices;

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
			return neglog2 / Math.log1p(movingAvgMeanReversionRatio());
		}

		public String toString() {
			return "adf = " + adf //
					+ ", hurst = " + hurst //
					+ ", movingAvgHalfLife = " + movingAvgHalfLife() //
					+ ", latestMovingAverage = " + latestMovingAverage();
		}
	}

}
