package suite.trade.analysis;

import java.util.Arrays;

import org.junit.Test;

import suite.math.linalg.Vector;
import suite.trade.Time;
import suite.trade.TimeRange;
import suite.trade.data.TradeCfg;
import suite.trade.data.TradeCfgImpl;
import suite.util.To;

public class FixedWindowFracDiffTest {

	private TradeCfg cfg = new TradeCfgImpl();
	private Vector vec = new Vector();

	// Advances in Financial Machine Learning, Marcos Lopez de Prado, 5.5
	@Test
	public void test() {
		var symbol = "2800.HK";
		var d = .5d;
		var window = 19;
		var period = TimeRange.of(Time.of(2005, 1, 1), TimeRange.max);
		var ts = cfg.dataSource(symbol).range(period).closes;

		var weights = new float[window];
		var weight = 1d;

		for (var k = 0; k < window;) {
			weights[k++] = (float) weight;
			weight *= -(d - k + 1) / k;
		}

		var fracDiff = To.vector(ts.length - window, i -> vec.convolute(window, ts, i, weights, window));

		System.out.println(Arrays.toString(fracDiff));
	}

}
