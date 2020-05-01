package suite.math.ts;

import org.junit.jupiter.api.Test;
import suite.trade.Time;
import suite.trade.TimeRange;
import suite.trade.data.TradeCfg;
import suite.trade.data.TradeCfgImpl;
import suite.ts.TimeSeries;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class TimeSeriesTest {

	private TradeCfg cfg = new TradeCfgImpl();
	private TimeSeries ts = new TimeSeries();

	@Test
	public void testFixedWindowFracDiff() {
		var symbol = "2800.HK";
		var d = .5d;
		var window = 19;
		var period = TimeRange.of(Time.of(2005, 1, 1), TimeRange.max);
		var fs = cfg.dataSource(symbol).range(period).closes;

		System.out.println(Arrays.toString(ts.fracDiff(fs, d, window)));
	}

	@Test
	public void testSharpeRatio() {
		var period = TimeRange.of(Time.of(2016, 1, 1), Time.of(2017, 5, 1));
		var ds = cfg.dataSource("0002.HK").range(period);
		var sharpe = ts.returnsStatDailyAnnualized(ds.prices).sharpeRatio();
		System.out.println("sharpe = " + sharpe);
		assertTrue(.04d < sharpe);
	}

}
