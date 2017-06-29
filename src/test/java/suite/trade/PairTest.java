package suite.trade;

import static org.junit.Assert.assertTrue;

import org.junit.Test;

import suite.math.stat.Statistic;
import suite.math.stat.Statistic.LinearRegression;
import suite.primitive.streamlet.IntStreamlet;
import suite.primitive.streamlet.LngStreamlet;
import suite.trade.data.Configuration;
import suite.trade.data.ConfigurationImpl;
import suite.trade.data.DataSource;

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
		LngStreamlet dates0 = LngStreamlet.of(ds0.dates);
		LngStreamlet dates1 = LngStreamlet.of(ds1.dates);
		long[] tradeDates = LngStreamlet.concat(dates0, dates1).distinct().sort().toArray();
		float[] prices0 = ds0.align(tradeDates).prices;
		float[] prices1 = ds1.align(tradeDates).prices;
		int length = prices0.length;
		float[][] x = IntStreamlet.range(length).map(i -> new float[] { prices0[i], 1f, }).toArray(float[].class);
		float[] y = prices1;
		LinearRegression lr = statistic.linearRegression(x, y);
		System.out.println(symbol0 + " -> " + symbol1 + lr);
		assertTrue(.4d < lr.r2);
	}

}
