package suite.trade;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static suite.util.Streamlet_.forInt;

import org.junit.jupiter.api.Test;

import primal.primitive.LngMoreVerbs.ConcatLng;
import primal.primitive.LngMoreVerbs.ReadLng;
import primal.primitive.adt.pair.FltObjPair;
import suite.math.linalg.Vector;
import suite.math.numeric.Statistic;
import suite.trade.data.TradeCfg;
import suite.trade.data.TradeCfgImpl;

/**
 * Finds the period of various stocks using FFT.
 *
 * @author ywsing
 */
public class PairTest {

	private TradeCfg cfg = new TradeCfgImpl();
	private Statistic statistic = new Statistic();
	private Vector vec = new Vector();

	@Test
	public void test() {
		var period = TimeRange.of(Time.of(2016, 1, 1), Time.of(2017, 1, 1));
		// test(period, "0005.HK", "2888.HK");
		test(period, "0341.HK", "0052.HK");
	}

	private void test(TimeRange period, String symbol0, String symbol1) {
		var ds0 = cfg.dataSource(symbol0, period);
		var ds1 = cfg.dataSource(symbol1, period);
		var ts0 = ReadLng.from(ds0.ts);
		var ts1 = ReadLng.from(ds1.ts);
		var tradeTimes = ConcatLng.of(ts0, ts1).distinct().sort().toArray();
		var prices0 = ds0.alignBeforePrices(tradeTimes).prices;
		var prices1 = ds1.alignBeforePrices(tradeTimes).prices;
		var length = prices0.length;

		var lr = statistic.linearRegression(forInt(length).map(i -> FltObjPair.of(prices1[i], vec.of(prices0[i], 1f))));

		System.out.println(symbol0 + " -> " + symbol1 + lr);
		assertTrue(.4d < lr.r2);
	}

}
