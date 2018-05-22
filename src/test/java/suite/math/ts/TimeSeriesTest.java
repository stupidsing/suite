package suite.math.ts;

import static org.junit.Assert.assertTrue;

import org.junit.Test;

import suite.trade.Time;
import suite.trade.TimeRange;
import suite.trade.data.TradeCfg;
import suite.trade.data.TradeCfgImpl;
import suite.ts.TimeSeries;

public class TimeSeriesTest {

	private TradeCfg cfg = new TradeCfgImpl();
	private TimeSeries ts = new TimeSeries();

	@Test
	public void testSharpeRatio() {
		var period = TimeRange.of(Time.of(2016, 1, 1), Time.of(2017, 5, 1));
		var ds = cfg.dataSource("0002.HK").range(period);
		var sharpe = ts.returnsStatDailyAnnualized(ds.prices).sharpeRatio();
		System.out.println("sharpe = " + sharpe);
		assertTrue(.04d < sharpe);
	}

}
