package suite.trade;

import static org.junit.Assert.assertTrue;

import org.junit.Test;

import suite.math.stat.Statistic;
import suite.math.stat.Statistic.LinearRegression;
import suite.streamlet.Read;
import suite.streamlet.Streamlet;
import suite.trade.data.Configuration;
import suite.trade.data.ConfigurationImpl;
import suite.trade.data.DataSource;
import suite.util.Object_;

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
		DataSource dataSource0 = cfg.dataSource(symbol0, period);
		DataSource dataSource1 = cfg.dataSource(symbol1, period);
		Streamlet<String> dates0 = Read.from(dataSource0.dates);
		Streamlet<String> dates1 = Read.from(dataSource1.dates);
		String[] tradeDates = Streamlet.concat(dates0, dates1).distinct().sort(Object_::compare).toArray(String.class);
		float[] prices0 = dataSource0.align(tradeDates).prices;
		float[] prices1 = dataSource1.align(tradeDates).prices;
		int length = prices0.length;
		float[][] x = Read.range(length).map(i -> new float[] { prices0[i], 1f, }).toArray(float[].class);
		float[] y = prices1;
		LinearRegression lr = statistic.linearRegression(x, y);
		System.out.println(symbol0 + " -> " + symbol1 + lr);
		assertTrue(.4d < lr.r2);
	}

}
