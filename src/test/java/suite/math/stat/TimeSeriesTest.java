package suite.math.stat;

import static org.junit.Assert.assertTrue;

import org.junit.Test;

import suite.trade.DatePeriod;
import suite.trade.Time;
import suite.trade.data.ConfigurationImpl;
import suite.trade.data.DataSource;

public class TimeSeriesTest {

	@Test
	public void testSharpeRatio() {
		ConfigurationImpl cfg = new ConfigurationImpl();
		TimeSeries ts = new TimeSeries();

		DatePeriod period = DatePeriod.of(Time.of(2016, 1, 1), Time.of(2017, 5, 1));
		DataSource ds = cfg.dataSource("0002.HK").range(period);
		double sharpe = ts.returnsStatDailyAnnualized(ds.prices).sharpeRatio();
		System.out.println("sharpe = " + sharpe);
		assertTrue(.04d < sharpe);
	}

}
