package suite.game;

import org.junit.Test;

import suite.streamlet.Read;
import suite.trade.TimeRange;
import suite.trade.data.TradeCfg;
import suite.trade.data.TradeCfgImpl;

public class PlottyTest {

	private Plotty pl = new Plotty();
	private TradeCfg cfg = new TradeCfgImpl();

	@Test
	public void test0005() {
		var period = TimeRange.threeYears();
		var ds = cfg.dataSource("0005.HK", period).range(period).validate();
		pl.plot(Read.each(ds.closes));
	}

	@Test
	public void testPlot() {
		var d0 = new float[] { 10f, 15f, 13f, 17f, };
		var d1 = new float[] { 16f, 5f, 11f, 9f, };
		pl.plot(Read.each(d0, d1));
	}

}
