package suite.ts;

import static java.lang.Math.expm1;
import static java.lang.Math.log1p;
import static java.lang.Math.max;

import suite.math.numeric.Statistic;
import suite.math.numeric.Statistic.MeanVariance;
import suite.primitive.Int_Dbl;
import suite.trade.Trade_;
import suite.util.To;

public class CalculateReturns {

	private static CalculateReturns me = new CalculateReturns();

	private Statistic stat = new Statistic();
	private TimeSeries ts = new TimeSeries();

	/**
	 * Simulates trading using a single instrument.
	 * 
	 * @param fun a function returns the holding position at a specified time.
	 * @return a strategy.
	 */
	public BuySell buySell(Int_Dbl fun) {
		return fun::apply;
	}

	public interface BuySell extends Int_Dbl {
		/**
		 * @return a new strategy that would never short.
		 */
		public default BuySell longOnly() {
			return d -> max(0d, apply(d));
		}

		/**
		 * Adjusts long ratio or leverage linearly.
		 * 
		 * @param a long ratio base.
		 * @param b leverage scaling factor.
		 * @return a new strategy.
		 */
		public default BuySell scale(double a, double b) {
			return d -> a + b * apply(d);
		}

		/**
		 * Specify amount of data to skip before the trading strategy actually starts.
		 * 
		 * @param s how many elements to skip.
		 * @return a new strategy.
		 */
		public default BuySell start(int s) {
			return d -> s <= d ? apply(d) : 0d;
		}

		/**
		 * Simulates the strategy without re-investing.
		 * 
		 * @param prices input prices.
		 * @return strategy returns.
		 */
		public default Returns engage(float[] prices) {
			return me.engage_(prices, To.vector(prices.length, this));
		}

		/**
		 * Simulates the strategy with re-investing.
		 * 
		 * @param prices input prices.
		 * @return strategy returns.
		 */
		public default Returns invest(float[] prices) {
			return me.invest_(prices, To.vector(prices.length, this));
		}
	}

	private Returns engage_(float[] prices, float[] holds) {
		var length = prices.length;
		var returns = new float[length];
		var val = 1d;
		returns[0] = (float) val;
		for (var d = 1; d < length; d++)
			returns[d] = (float) (val += holds[d] * (prices[d] - prices[d - 1]));
		return new Returns(returns);
	}

	private Returns invest_(float[] prices, float[] holds) {
		var length = prices.length;
		var returns = new float[length];
		var val = 1d;
		returns[0] = (float) val;
		for (var d = 1; d < length; d++)
			returns[d] = (float) (val *= 1d + holds[d] * Quant.return_(prices[d - 1], prices[d]));
		return new Returns(returns);
	}

	public class Returns {
		private float[] vals;
		private float[] returns;
		private MeanVariance rmv;

		private Returns(float[] vals) {
			this.vals = vals;
			returns = ts.returns(vals);
			rmv = stat.meanVariance(returns);
		}

		public String toString() {
			var return_ = return_();
			var yearPeriod = Trade_.nTradeDaysPerYear / (double) vals.length;
			return "o/c =" //
					+ " rtn:" + To.string(return_) //
					+ " cagr:" + To.string(expm1(log1p(return_) * yearPeriod)) //
					+ " sharpe:" + To.string(sharpe()) //
					+ " dist:" + rmv;
		}

		private double return_() {
			return Quant.return_(vals[0], vals[vals.length - 1]);
		}

		public double sharpe() {
			return rmv.mean / rmv.standardDeviation();
		}
	}

}
