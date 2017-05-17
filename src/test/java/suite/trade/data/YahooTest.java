package suite.trade.data;

import static org.junit.Assert.assertTrue;

import java.time.LocalDate;

import org.junit.Test;

import suite.trade.DatePeriod;

public class YahooTest {

	private Yahoo yahoo = new Yahoo();

	@Test
	public void test() {
		DataSource dataSource = yahoo.dataSourceYql("0005.HK", DatePeriod.of(LocalDate.of(2016, 1, 1), LocalDate.of(2017, 1, 1)));
		assertTrue(0 < dataSource.dates.length);
		assertTrue(0 < dataSource.prices.length);
	}

}
