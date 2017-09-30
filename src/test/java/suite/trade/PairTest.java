package suite.trade;

import static org.junit.Assert.assertTrue;

import org.junit.Test;

import suite.math.stat.Statistic;
import suite.math.stat.Statistic.LinearRegression;
import suite.primitive.streamlet.LngStreamlet;
import suite.trade.data.Configuration;
import suite.trade.data.ConfigurationImpl;
import suite.trade.data.DataSource;
import suite.util.To;

/**
 * Finds the period of various stocks using FFT.
 *
 * @author ywsing
 */
public class PairTest {

	private Configuration cfg = new ConfigurationImpl();
	private Statistic statistic = new Statistic();

	@Test
	public void test() {
		TimeRange period = TimeRange.of(Time.of(2016, 1, 1), Time.of(2017, 1, 1));
		// test(period, "0005.HK", "2888.HK");
		test(period, "0341.HK", "0052.HK");
	}

	private void test(TimeRange period, String symbol0, String symbol1) {
		DataSource ds0 = cfg.dataSource(symbol0, period);
		DataSource ds1 = cfg.dataSource(symbol1, period);
		LngStreamlet ts0 = LngStreamlet.of(ds0.ts);
		LngStreamlet ts1 = LngStreamlet.of(ds1.ts);
		long[] tradeTimes = LngStreamlet.concat(ts0, ts1).distinct().sort().toArray();
		float[] prices0 = ds0.alignBeforePrices(tradeTimes).prices;
		float[] prices1 = ds1.alignBeforePrices(tradeTimes).prices;
		int length = prices0.length;
		float[][] x = To.array(float[].class, length, i -> new float[] { prices0[i], 1f, });
		float[] y = prices1;
		LinearRegression lr = statistic.linearRegression(x, y);
		System.out.println(symbol0 + " -> " + symbol1 + lr);
		assertTrue(.4d < lr.r2);
	}

}
