package suite.game;

import static java.lang.Math.exp;

import org.junit.jupiter.api.Test;

import primal.MoreVerbs.Read;
import primal.primitive.FltMoreVerbs.ReadFlt;
import suite.trade.TimeRange;
import suite.trade.data.TradeCfg;
import suite.trade.data.TradeCfgImpl;
import suite.util.To;

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
	public void testLogistic() {
		var logistics = ReadFlt.from(new float[] { .1f, 1f, 10f }).map(theta -> To.vector(1200, i -> {
			var x = (i - 600) * (1d / 100);
			return 1d / (1d + exp(-theta * x));
		}));
		pl.plot(logistics);
	}

	@Test
	public void testPlot() {
		var d0 = new float[] { 10f, 15f, 13f, 17f, };
		var d1 = new float[] { 16f, 5f, 11f, 9f, };
		pl.plot(Read.each(d0, d1));
	}

}
