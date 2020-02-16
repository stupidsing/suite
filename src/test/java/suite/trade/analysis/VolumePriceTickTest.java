package suite.trade.analysis;

import org.junit.jupiter.api.Test;

import suite.math.linalg.Vector;
import suite.trade.Time;
import suite.trade.TimeRange;
import suite.trade.data.DataSource;
import suite.trade.data.TradeCfg;
import suite.trade.data.TradeCfgImpl;
import suite.ts.CalculateReturns;
import suite.ts.Quant;

public class VolumePriceTickTest {

	private String symbol = "1299.HK";
	private TimeRange period = TimeRange.of(Time.of(2005, 1, 1), TimeRange.max);
	// TimeRange.of(Time.of(2013, 1, 1), Time.of(2014, 1, 1));
	// TimeRange.threeYears();

	private CalculateReturns cr = new CalculateReturns();
	private MovingAverage ma = new MovingAverage();
	private TradeCfg cfg = new TradeCfgImpl();
	private Vector vec = new Vector();

	@Test
	public void test() {
		analyze(cfg.dataSource(symbol).range(period), 384);
	}

	private void analyze(DataSource ds, int vpl) {
		var vpt = new VolumePriceTick(ds.closes, ds.volumes, vpl);
		var prices = Boolean.TRUE ? vpt.forward() : vpt.backward();

		for (var days = 1; days < 8; days++) {
			var ma_a = ma.movingAvg(prices, 1);
			var ma_b = ma.movingAvg(prices, days);
			var returns = cr.buySell(d -> {
				var last = d - 1;
				return Quant.sign(ma_a[last], ma_b[last]);
			}).start(1).invest(prices);
			System.out.println("ma[" + days + "] = " + returns.sharpe());
		}
	}

	private class VolumePriceTick {
		private float[] vols;
		private double vpd;
		private float[] prices0, prices1;
		private double vp0, vp1;

		private VolumePriceTick(float[] prices, float[] vols, int vpl) {
			this.vols = vols;
			vpd = vec.dot(prices, vols) / vpl;
			prices0 = prices;
			prices1 = new float[vpl];
		}

		private float[] forward() {
			var t0 = 0;

			for (var t1 = 0; t1 < prices1.length; t1++) {
				prices1[t1] = prices0[t0];
				vp1 += vpd;
				while (vp0 < vp1 && t0 < prices0.length) {
					vp0 += prices0[t0] * vols[t0];
					t0++;
				}
			}

			return prices1;
		}

		private float[] backward() {
			var t0 = prices0.length - 1;

			for (var t1 = prices1.length - 1; 0 <= t1; t1--) {
				prices1[t1] = prices0[t0];
				vp1 += vpd;
				while (vp0 < vp1 && 0 <= t0) {
					vp0 += prices0[t0] * vols[t0];
					t0--;
				}
			}

			return prices1;
		}
	}

}
