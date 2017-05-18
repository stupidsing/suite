package suite.trade.data;

import static org.junit.Assert.assertTrue;

import java.time.LocalDate;
import java.util.function.BiFunction;

import org.junit.Test;

import suite.trade.DatePeriod;

public class YahooTest {

	private Yahoo yahoo = new Yahoo();

	@Test
	public void testL1() {
		test(yahoo::dataSourceL1);
	}

	@Test
	public void testYql() {
		test(yahoo::dataSourceYql);
	}

	private void test(BiFunction<String, DatePeriod, DataSource> fun) {
		DataSource dataSource = fun.apply("0005.HK", DatePeriod.of(LocalDate.of(2016, 1, 1), LocalDate.of(2017, 1, 1)));
		System.out.println(dataSource);

		int datesLength = dataSource.dates.length;
		int pricesLength = dataSource.prices.length;
		assertTrue(datesLength == pricesLength);
		assertTrue(0 < datesLength);
	}

}
