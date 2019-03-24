package suite.trade.analysis;

import static suite.util.Friends.abs;
import static suite.util.Friends.expm1;
import static suite.util.Friends.forInt;
import static suite.util.Friends.log1p;
import static suite.util.Friends.max;

import java.util.Arrays;

import org.junit.Test;

import suite.math.Tanh;
import suite.math.linalg.VirtualVector;
import suite.math.numeric.Statistic;
import suite.math.numeric.Statistic.MeanVariance;
import suite.math.transform.DiscreteCosineTransform;
import suite.os.Log_;
import suite.primitive.Floats_;
import suite.primitive.IntPrimitives.Int_Obj;
import suite.primitive.Int_Dbl;
import suite.primitive.Ints_;
import suite.primitive.adt.pair.IntFltPair;
import suite.trade.Time;
import suite.trade.TimeRange;
import suite.trade.Trade_;
import suite.trade.data.DataSource;
import suite.trade.data.TradeCfg;
import suite.trade.data.TradeCfgImpl;
import suite.ts.BollingerBands;
import suite.ts.Quant;
import suite.ts.TimeSeries;
import suite.util.To;

// mvn test -Dtest=AnalyzeTimeSeriesTest#test
public class AnalyzeTimeSeriesTest {

	private static AnalyzeTimeSeriesTest me = new AnalyzeTimeSeriesTest();

	private String symbol = "2800.HK";
	private TimeRange period = TimeRange.of(Time.of(2005, 1, 1), TimeRange.max);
	// TimeRange.of(Time.of(2013, 1, 1), Time.of(2014, 1, 1));
	// TimeRange.threeYears();

	private BollingerBands bb = new BollingerBands();
	private TradeCfg cfg = new TradeCfgImpl();
	private DiscreteCosineTransform dct = new DiscreteCosineTransform();
	private MarketTiming mt = new MarketTiming();
	private MovingAverage ma = new MovingAverage();
	private Statistic stat = new Statistic();
	private TimeSeries ts = new TimeSeries();

	@Test
	public void test() {
		analyze(cfg.dataSource(symbol).range(period));
	}

	private void analyze(DataSource ds) {
		var length = ds.ts.length;
		var ops = ds.opens;
		var cls = ds.closes;
		var ocgs = To.vector(length, i -> cls[i] - ops[i]);
		var cogs = To.vector(length, i -> ops[i] - cls[max(0, i - 1)]);
		Log_.info("open/close gap = " + stat.meanVariance(ocgs));
		Log_.info("close/open gap = " + stat.meanVariance(cogs));
		Log_.info("ocg/cog covariance = " + stat.correlation(ocgs, cogs));
		analyze(ds.prices);
	}

	private void analyze(float[] prices) {
		var length = prices.length;
		var log2 = Quant.log2trunc(length);
		var fds = dct.dct(Arrays.copyOfRange(prices, length - log2, length));
		var returns = ts.returns(prices);
		var logPrices = To.vector(prices, Math::log);
		var logReturns = ts.differences(1, logPrices);
		var rmv = stat.meanVariance(returns);
		var variance = rmv.variance;
		var kelly = rmv.mean / variance;
		var max = IntFltPair.of(Integer.MIN_VALUE, Float.MIN_VALUE);

		for (var i = 4; i < fds.length; i++) {
			var f = abs(fds[i]);
			if (max.t1 < f)
				max.update(i, f);
		}

		Int_Obj<BuySell> momFun = n -> {
			var d0 = 1 + n;
			var d1 = 1;
			return buySell(d -> Quant.sign(prices[d - d0], prices[d - d1])).start(d0);
		};

		Int_Obj<BuySell> revert = d -> momFun.apply(d).scale(0d, -1d);
		Int_Obj<BuySell> trend_ = d -> momFun.apply(d).scale(0d, +1d);
		var reverts = To.array(8, BuySell.class, revert);
		var trends_ = To.array(8, BuySell.class, trend_);
		var tanh = buySell(d -> Tanh.tanh(3.2d * reverts[1].apply(d)));
		var holds = mt.hold(prices, 1f, 1f, 1f);
		var ma200 = ma.movingAvg(prices, 200);
		var mat = buySell(d -> {
			var last = d - 1;
			return Quant.sign(ma200[last], prices[last]);
		}).start(1).longOnly();
		var mt_ = buySell(d -> holds[d]);

		var bbmv = bb.meanVariances(VirtualVector.of(logReturns), 9, 0);
		var bbmean = bbmv.t0;
		var bbvariances = bbmv.t1;

		var ms2 = buySell(d -> {
			var last = d - 1;
			var ref = last - 250;
			var mean = bbmean[last];
			return Quant.sign(logPrices[last], logPrices[ref] - bbvariances[last] / (2d * mean * mean));
		}).start(1 + 250);

		Log_.info("" //
				+ "\nsymbol = " + symbol //
				+ "\nlength = " + length //
				+ "\nnYears = " + length * Trade_.invTradeDaysPerYear //
				+ "\nups = " + Floats_.of(returns).filter(return_ -> 0f <= return_).size() //
				+ "\ndct period = " + max.t0 //
				+ forInt(10).map(d -> "dct component [" + d + "d] = " + fds[d]) //
				+ "\nreturn kelly = " + kelly //
				+ "\nreturn skew = " + stat.skewness(returns) //
				+ "\nreturn kurt = " + stat.kurtosis(returns) //
				+ Ints_ //
						.of(1, 2, 4, 8, 16, 32) //
						.map(d -> "mean reversion ols [" + d + "d] = " + ts.meanReversion(prices, d).coefficients[0]) //
				+ Ints_ //
						.of(4, 16) //
						.map(d -> "variance ratio [" + d + "d over 1d] = " + ts.varianceRatio(prices, d)) //
				+ "\nreturn hurst = " + ts.hurst(prices, prices.length / 2) //
				+ "\nhold " + buySell(d -> 1d).invest(prices) //
				+ "\nkelly " + buySell(d -> kelly).invest(prices) //
				+ "\nma200 trend " + mat.invest(prices) //
				+ forInt(1, 8).map(d -> "revert [" + d + "d] " + reverts[d].invest(prices)) //
				+ forInt(1, 8).map(d -> "trend_ [" + d + "d] " + trends_[d].invest(prices)) //
				+ forInt(1, 8).map(d -> "revert [" + d + "d] long-only " + reverts[d].longOnly().invest(prices)) //
				+ forInt(1, 8).map(d -> "trend_ [" + d + "d] long-only " + trends_[d].longOnly().invest(prices)) //
				+ "\nms2 " + ms2.invest(prices) //
				+ "\nms2 long-only " + ms2.longOnly().invest(prices) //
				+ "\ntanh " + tanh.invest(prices) //
				+ "\ntimed " + mt_.invest(prices) //
				+ "\ntimed long-only " + mt_.longOnly().invest(prices));
	}

	private BuySell buySell(Int_Dbl fun) {
		return fun::apply;
	}

	public interface BuySell extends Int_Dbl {
		public default BuySell longOnly() {
			return d -> max(0d, apply(d));
		}

		public default BuySell scale(double a, double b) {
			return d -> a + b * apply(d);
		}

		public default BuySell start(int s) {
			return d -> s <= d ? apply(d) : 0d;
		}

		public default Returns engage(float[] prices) {
			return me.engage_(prices, To.vector(prices.length, this));
		}

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
			var return_ = return_();
			var yearPeriod = Trade_.nTradeDaysPerYear / (double) vals.length;
			return "o/c =" //
					+ " rtn:" + To.string(return_) //
					+ " yearRtn:" + To.string(expm1(log1p(return_) * yearPeriod)) //
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
