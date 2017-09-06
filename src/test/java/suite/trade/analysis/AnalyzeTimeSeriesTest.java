package suite.trade.analysis;

import java.util.Arrays;

import org.junit.Test;

import suite.math.Tanh;
import suite.math.stat.Quant;
import suite.math.stat.Statistic;
import suite.math.stat.Statistic.MeanVariance;
import suite.math.stat.TimeSeries;
import suite.math.transform.DiscreteCosineTransform;
import suite.os.LogUtil;
import suite.primitive.Floats_;
import suite.primitive.IntPrimitives.Int_Obj;
import suite.primitive.Int_Dbl;
import suite.primitive.Int_Flt;
import suite.primitive.Ints_;
import suite.primitive.adt.pair.IntFltPair;
import suite.primitive.streamlet.FltStreamlet;
import suite.primitive.streamlet.IntStreamlet;
import suite.streamlet.As;
import suite.trade.Time;
import suite.trade.TimeRange;
import suite.trade.Trade_;
import suite.trade.data.Configuration;
import suite.trade.data.ConfigurationImpl;
import suite.trade.data.DataSource;
import suite.util.To;

public class AnalyzeTimeSeriesTest {

	private static AnalyzeTimeSeriesTest me = new AnalyzeTimeSeriesTest();

	private String symbol = "^HSI";
	private TimeRange period = TimeRange.of(Time.of(2005, 1, 1), TimeRange.max);
	// TimeRange.of(Time.of(2013, 1, 1), Time.of(2014, 1, 1));
	// TimeRange.threeYears();

	private Configuration cfg = new ConfigurationImpl();
	private DiscreteCosineTransform dct = new DiscreteCosineTransform();
	private MarketTiming marketTiming = new MarketTiming();
	private Statistic stat = new Statistic();
	private TimeSeries ts = new TimeSeries();

	@Test
	public void test() {
		analyze(cfg.dataSource(symbol).range(period));
	}

	private void analyze(DataSource ds) {
		float[] ops = ds.opens;
		float[] cls = ds.closes;
		float[] ocgs = Floats_.toArray(ds.ts.length, i -> cls[i] - ops[i]);
		float[] cogs = Floats_.toArray(ds.ts.length, i -> ops[i] - cls[Math.max(0, i - 1)]);
		LogUtil.info("open/close gap = " + stat.meanVariance(ocgs));
		LogUtil.info("close/open gap = " + stat.meanVariance(cogs));
		LogUtil.info("ocg/cog covariance = " + stat.correlation(ocgs, cogs));
		analyze(ds.prices);
	}

	private void analyze(float[] prices) {
		int length = prices.length;
		int log2 = Quant.log2trunc(length);
		double nYears = length * Trade_.invTradeDaysPerYear;

		float[] fds = dct.dct(Arrays.copyOfRange(prices, length - log2, length));
		float[] returns = ts.returns(prices);
		MeanVariance rmv = stat.meanVariance(returns);
		double variance = rmv.variance;
		double kelly = rmv.mean / variance;
		IntFltPair max = IntFltPair.of(Integer.MIN_VALUE, Float.MIN_VALUE);

		for (int i = 4; i < fds.length; i++) {
			float f = Math.abs(fds[i]);
			if (max.t1 < f)
				max = IntFltPair.of(i, f);
		}

		Int_Obj<BuySell> momFun = n -> {
			int d0 = 1 + n;
			int d1 = 1;
			return buySell(d -> Quant.sign(prices[d - d0], prices[d - d1])).start(d0);
		};

		BuySell mom = momFun.apply(1);
		BuySell revert = mom.scale(-1f);
		BuySell trend_ = mom.scale(+1f);
		BuySell tanh = buySell(d -> Tanh.tanh(-3.2d * mom.apply(d)));
		float[] holds = marketTiming.hold(prices);
		BuySell mt_ = buySell(d -> holds[d]);

		LogUtil.info("" //
				+ "\nsymbol = " + symbol //
				+ "\nlength = " + length //
				+ "\nnYears = " + nYears //
				+ "\nups = " + FltStreamlet.of(returns).filter(return_ -> 0f <= return_).size() //
				+ "\ndct period = " + max.t0 //
				+ Ints_ //
						.range(10) //
						.map(d -> "\ndct component, " + d + " days = " + fds[d]) //
						.collect(As::joined) //
				+ "\nreturn yearly sharpe = " + rmv.mean / Math.sqrt(variance / nYears) //
				+ "\nreturn kelly = " + kelly //
				+ "\nreturn skew = " + stat.skewness(returns) //
				+ "\nreturn kurt = " + stat.kurtosis(returns) //
				+ IntStreamlet //
						.of(1, 2, 4, 8, 16, 32) //
						.map(d -> "\nmean reversion ols, " + d + " days = " + ts.meanReversion(prices, d).coefficients[0]) //
						.collect(As::joined) //
				+ IntStreamlet //
						.of(4, 16) //
						.map(d -> "\nvariance ratio, " + d + " days over 1 day = " + ts.varianceRatio(prices, d)) //
						.collect(As::joined) //
				+ "\nhalf " + buySell(d -> .5d).invest(prices) //
				+ "\nhold " + buySell(d -> 1d).invest(prices) //
				+ "\nkelly " + buySell(d -> kelly).invest(prices) //
				+ "\nrevert " + revert.invest(prices) //
				+ "\nrevert long-only " + revert.longOnly().invest(prices) //
				+ "\ntrend_ " + trend_.invest(prices) //
				+ "\ntrend_ long-only " + trend_.longOnly().invest(prices) //
				+ "\ntanh " + tanh.invest(prices) //
				+ "\ntimed " + mt_.invest(prices));
	}

	private BuySell buySell(Int_Dbl fun) {
		return d -> (float) fun.apply(d);
	}

	public interface BuySell extends Int_Flt {
		public default BuySell longOnly() {
			return d -> Math.max(0f, apply(d));
		}

		public default BuySell scale(float scale) {
			return d -> scale * apply(d);
		}

		public default BuySell start(int s) {
			return d -> s <= d ? apply(d) : 0f;
		}

		public default Returns engage(float[] prices) {
			return me.engage_(prices, Floats_.toArray(prices.length, this));
		}

		public default Returns invest(float[] prices) {
			return me.invest_(prices, Floats_.toArray(prices.length, this));
		}
	}

	private Returns engage_(float[] prices, float[] holds) {
		int length = prices.length;
		float[] returns = new float[length];
		double val;
		returns[0] = (float) (val = 1d);
		for (int d = 1; d < length; d++)
			returns[d] = (float) (val += holds[d] * (prices[d] - prices[d - 1]));
		return new Returns(returns);
	}

	private Returns invest_(float[] prices, float[] holds) {
		int length = prices.length;
		float[] returns = new float[length];
		double val;
		returns[0] = (float) (val = 1d);
		for (int d = 1; d < length; d++)
			returns[d] = (float) (val *= 1d + holds[d] * Quant.return_(prices[d - 1], prices[d]));
		return new Returns(returns);
	}

	private class Returns {
		private float[] vals;
		private float[] returns;
		private MeanVariance rmv;

		private Returns(float[] vals) {
			this.vals = vals;
			returns = ts.returns(vals);
			rmv = stat.meanVariance(returns);
		}

		public String toString() {
			double return_ = return_();
			double yearPeriod = Trade_.nTradeDaysPerYear / (double) vals.length;
			return "o/c =" //
					+ " rtn:" + To.string(return_) //
					+ " yearRtn:" + To.string(Math.expm1(Math.log1p(return_) * yearPeriod)) //
					+ " sharpe:" + To.string(sharpe()) //
					+ " dist:" + rmv;
		}

		private double return_() {
			return Quant.return_(vals[0], vals[vals.length - 1]);
		}

		private double sharpe() {
			return rmv.mean / rmv.standardDeviation();
		}
	}

}
