package suite.trade.analysis;

import java.util.Arrays;

import org.junit.Test;

import suite.math.Tanh;
import suite.math.stat.BollingerBands;
import suite.math.stat.Quant;
import suite.math.stat.Statistic;
import suite.math.stat.Statistic.MeanVariance;
import suite.math.stat.TimeSeries;
import suite.math.transform.DiscreteCosineTransform;
import suite.os.LogUtil;
import suite.primitive.Floats_;
import suite.primitive.Int_Dbl;
import suite.primitive.Int_Flt;
import suite.primitive.adt.pair.IntFltPair;
import suite.trade.TimeRange;
import suite.trade.Trade_;
import suite.trade.data.Configuration;
import suite.trade.data.ConfigurationImpl;

public class AnalyzeTimeSeriesTest {

	private String symbol = "^HSI";
	private TimeRange period = TimeRange.threeYears();

	BollingerBands bb = new BollingerBands();
	private Configuration cfg = new ConfigurationImpl();
	private DiscreteCosineTransform dct = new DiscreteCosineTransform();
	private Statistic stat = new Statistic();
	private TimeSeries ts = new TimeSeries();

	@Test
	public void test() {
		float[] prices = cfg.dataSource(symbol).range(period).prices;
		int length = prices.length;
		double nYears = length * Trade_.invTradeDaysPerYear;
		int log2 = Quant.log2trunc(length);

		float[] fds = dct.dct(Arrays.copyOfRange(prices, length - log2, length));
		float[] returns = ts.returns(prices);
		MeanVariance rmv = stat.meanVariance(returns);
		double kelly = rmv.mean / rmv.variance;
		IntFltPair max = IntFltPair.of(Integer.MIN_VALUE, Float.MIN_VALUE);

		for (int i = 4; i < fds.length; i++) {
			float f = Math.abs(fds[i]);
			if (max.t1 < f)
				max = IntFltPair.of(i, f);
		}

		LogUtil.info("symbol = " + symbol);
		LogUtil.info("length = " + length);
		LogUtil.info("nYears = " + nYears);
		LogUtil.info("dct period = " + max.t0);
		for (int d = 0; d <= 10; d++)
			LogUtil.info("dct component, " + d + " days = " + fds[d]);
		LogUtil.info("return = " + Quant.return_(prices[0], prices[length - 1]));
		LogUtil.info("return sharpe = " + rmv.mean / rmv.standardDeviation()); // * Trade_.nTradeDaysPerYear
		LogUtil.info("return kelly = " + kelly);
		LogUtil.info("return skew = " + stat.skewness(returns));
		LogUtil.info("return kurt = " + stat.kurtosis(returns));
		for (int d : new int[] { 1, 2, 4, 8, 16, 32, })
			LogUtil.info("mean reversion ols, " + d + " days = " + ts.meanReversion(prices, d).coefficients[0]);
		for (int d : new int[] { 4, 16, })
			LogUtil.info("variance ratio, " + d + " days over 1 day = " + ts.varianceRatio(prices, d));

		BuySell rev = buySell(d -> prices[d - 2] < prices[d - 1] ? -1d : 1d).start(2);

		int d0 = 1 + 12;
		int d1 = 1;
		BuySell tanh = buySell(d -> Tanh.tanh(-3.2d * Quant.return_(prices[d - d0], prices[d - d1]))).start(d0);

		LogUtil.info("half outcome = " + buySell(d -> .5d).invest(prices));
		LogUtil.info("hold outcome = " + buySell(d -> 1d).invest(prices));
		LogUtil.info("kelly outcome = " + buySell(d -> kelly).invest(prices));
		LogUtil.info("rev outcome = " + rev.invest(prices));
		LogUtil.info("rev long-only outcome = " + rev.longOnly().invest(prices));
		LogUtil.info("tanh outcome = " + tanh.invest(prices));
	}

	private BuySell buySell(Int_Dbl fun) {
		return d -> (float) fun.apply(d);
	}

	public interface BuySell extends Int_Flt {
		public default BuySell longOnly() {
			return d -> {
				float r = apply(d);
				return 0f <= r ? r : 0f;
			};
		}

		public default BuySell start(int s) {
			return d -> s <= d ? apply(d) : 0f;
		}

		public default double engage(float[] prices) {
			return engage_(prices, Floats_.toArray(prices.length, this));
		}

		public default double invest(float[] prices) {
			return invest_(prices, Floats_.toArray(prices.length, this));
		}
	}

	private static double engage_(float[] prices, float[] holds) {
		double val = 0f;
		for (int d = 1; d < prices.length; d++)
			val += holds[d] * (prices[d] - prices[d - 1]);
		return val;
	}

	private static double invest_(float[] prices, float[] holds) {
		double val = 1d;
		for (int d = 1; d < prices.length; d++)
			val *= 1d + holds[d] * Quant.return_(prices[d - 1], prices[d]);
		return val;
	}

}
