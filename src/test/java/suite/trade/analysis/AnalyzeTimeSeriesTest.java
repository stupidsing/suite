package suite.trade.analysis;

import java.util.Arrays;

import org.junit.Test;

import suite.math.stat.BollingerBands;
import suite.math.stat.Quant;
import suite.math.stat.Statistic;
import suite.math.stat.Statistic.MeanVariance;
import suite.math.stat.TimeSeries;
import suite.math.transform.DiscreteCosineTransform;
import suite.os.LogUtil;
import suite.primitive.Floats_;
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
		LogUtil.info("return sharpe = " + rmv.mean / rmv.variance); // * Trade_.nTradeDaysPerYear
		LogUtil.info("return skew = " + stat.skewness(returns));
		LogUtil.info("return kurt = " + stat.kurtosis(returns));
		for (int d : new int[] { 1, 2, 4, 8, 16, 32, })
			LogUtil.info("mean reversion ols, " + d + " days = " + ts.meanReversion(prices, d).coefficients[0]);
		for (int d : new int[] { 4, 16, })
			LogUtil.info("variance ratio, " + d + " days over 1 day = " + ts.varianceRatio(prices, d));

		LogUtil.info("half outcome = " + outcome(prices, d -> .5f));
		LogUtil.info("rev outcome = " + outcome(prices, d -> 2 <= d && prices[d - 2] < prices[d - 1] ? -1f : 1f));
		LogUtil.info("rev long-only outcome = " + outcome(prices, d -> 2 <= d && prices[d - 2] < prices[d - 1] ? 0f : 1f));
	}

	private float outcome(float[] prices, Int_Flt strategy) {
		float[] holds = Floats_.toArray(prices.length, strategy);
		if (Boolean.TRUE)
			return investOutcome(prices, holds);
		else
			return fixedOutcome(prices, holds);
	}

	private float fixedOutcome(float[] prices, float[] holds) {
		float val = 0f;
		for (int d = 1; d < prices.length; d++)
			val += holds[d] * (prices[d] - prices[d - 1]);
		return val;
	}

	private float investOutcome(float[] prices, float[] holds) {
		double val = 1d;
		for (int d = 1; d < prices.length; d++)
			val *= 1d + holds[d] * Quant.return_(prices[d - 1], prices[d]);
		return (float) val;
	}

}
