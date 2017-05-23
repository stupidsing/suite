package suite.trade;

import java.time.LocalDate;
import java.util.Arrays;

import org.junit.Test;

import suite.math.stat.Statistic;
import suite.math.stat.Statistic.LinearRegression;
import suite.streamlet.Read;
import suite.streamlet.Streamlet;
import suite.trade.data.Configuration;
import suite.trade.data.ConfigurationImpl;
import suite.trade.data.DataSource;
import suite.util.Object_;
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
		String symbol0 = "0005.HK";
		String symbol1 = "2888.HK";
		DatePeriod period = DatePeriod.of(LocalDate.of(2016, 1, 1), LocalDate.of(2017, 1, 1));
		DataSource dataSource0 = cfg.dataSource(symbol0, period);
		DataSource dataSource1 = cfg.dataSource(symbol1, period);
		Streamlet<String> dates0 = Read.from(dataSource0.dates);
		Streamlet<String> dates1 = Read.from(dataSource1.dates);
		String[] tradeDates = Streamlet.concat(dates0, dates1).distinct().sort(Object_::compare).toArray(String.class);
		float[] prices0 = dataSource0.align(tradeDates).prices;
		float[] prices1 = dataSource1.align(tradeDates).prices;
		int length = prices0.length;
		float[][] x = Read.range(length).map(i -> new float[] { prices0[i], prices1[i], }).toArray(float[].class);
		float[] y = To.arrayOfFloats(length, i -> 1f);
		LinearRegression lr = statistic.linearRegression(x, y);
		System.out.println(Arrays.toString(tradeDates));
		System.out.println(Arrays.toString(prices0));
		System.out.println(Arrays.toString(prices1));
		System.out.println(lr);
	}

}
