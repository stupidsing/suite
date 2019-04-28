package suite.trade.analysis;

import java.util.Arrays;

import org.junit.Test;

import suite.trade.Time;
import suite.trade.TimeRange;
import suite.trade.data.TradeCfg;
import suite.trade.data.TradeCfgImpl;
import suite.ts.TimeSeries;

public class FixedWindowFracDiffTest {

	private TimeSeries ts = new TimeSeries();
	private TradeCfg cfg = new TradeCfgImpl();

	@Test
	public void test() {
		var symbol = "2800.HK";
		var d = .5d;
		var window = 19;
		var period = TimeRange.of(Time.of(2005, 1, 1), TimeRange.max);
		var fs = cfg.dataSource(symbol).range(period).closes;

		System.out.println(Arrays.toString(ts.fracDiff(fs, d, window)));
	}

}
