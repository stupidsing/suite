package suite.math;

import static org.junit.Assert.assertTrue;

import java.time.LocalDate;

import org.junit.Test;

import suite.trade.DataSource;
import suite.trade.DatePeriod;
import suite.trade.Yahoo;

public class TimeSeriesTest {

	@Test
	public void testSharpeRatio() {
		DatePeriod period = DatePeriod.of(LocalDate.of(2016, 1, 1), LocalDate.of(2017, 5, 1));
		DataSource ds = new Yahoo().dataSource("0002.HK").limit(period);
		double sharpe = new TimeSeries().sharpeRatio(ds.prices, period.nYears());
		System.out.println("sharpe = " + sharpe);
		assertTrue(.04d < sharpe);
	}

}
