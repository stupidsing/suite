package suite.math;

import static org.junit.Assert.assertTrue;

import java.time.LocalDate;

import org.junit.Test;

import suite.trade.DatePeriod;
import suite.trade.data.DataSource;
import suite.trade.data.Yahoo;

public class TimeSeriesTest {

	@Test
	public void testSharpeRatio() {
		DatePeriod period = DatePeriod.of(LocalDate.of(2016, 1, 1), LocalDate.of(2017, 5, 1));
		DataSource ds = new Yahoo().dataSource("0002.HK").range(period);
		double sharpe = new TimeSeries().sharpeRatio(ds.prices, period.nYears());
		System.out.println("sharpe = " + sharpe);
		assertTrue(.04d < sharpe);
	}

}
