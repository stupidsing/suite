package suite.trade.analysis;

import org.junit.Test;

import suite.math.linalg.Vector;
import suite.trade.Time;
import suite.trade.TimeRange;
import suite.trade.data.DataSource;
import suite.trade.data.TradeCfg;
import suite.trade.data.TradeCfgImpl;
import suite.ts.CalculateReturns;

// mvn test -Dtest=AnalyzeTimeSeriesTest#test
public class VolumePriceTickTest {

	private String symbol = "2800.HK";
	private TimeRange period = TimeRange.of(Time.of(2005, 1, 1), TimeRange.max);
	// TimeRange.of(Time.of(2013, 1, 1), Time.of(2014, 1, 1));
	// TimeRange.threeYears();

	private CalculateReturns cr = new CalculateReturns();
	private MovingAverage ma = new MovingAverage();
	private TradeCfg cfg = new TradeCfgImpl();
	private Vector vec = new Vector();

	@Test
	public void test() {
		analyze(cfg.dataSource(symbol).range(period), 1024);
	}

	private void analyze(DataSource ds, int vpl) {
		var vpt = new VolumePriceTick(ds.closes, ds.volumes, vpl);
		var prices = Boolean.TRUE ? vpt.forward() : vpt.backward();

		for (var days = 1; days < 8; days++) {
			var ma_ = ma.movingAvg(prices, days);
			var returns = cr.buySell(t -> prices[t] < ma_[t] ? -1d : 1d).invest(prices);
			System.out.println("ma[" + days + "] = " + returns.sharpe());
		}
	}

	private class VolumePriceTick {
		private float[] prices;
		private float[] vols;
		private int vpl;
		private double vpd;

		private VolumePriceTick(float[] prices, float[] vols, int vpl) {
			this.prices = prices;
			this.vols = vols;
			this.vpl = vpl;

			vpd = vec.dot(prices, vols) / vpl;
		}

		private float[] forward() {
			var prices0 = prices;
			var prices1 = new float[vpl];
			var vp0 = 0d;
			var vp1 = 0d;
			var t0 = 0;

			for (var t1 = 0; t1 < vpl; t1++) {
				prices1[t1] = prices0[t0];
				vp1 += vpd;
				while (vp0 < vp1 && t0 < prices.length) {
					vp0 += prices0[t0] * vols[t0];
					t0++;
				}
			}

			return prices1;
		}

		private float[] backward() {
			var prices0 = prices;
			var prices1 = new float[vpl];
			var pv0 = 0d;
			var pv1 = 0d;
			var t0 = prices.length - 1;

			for (var t1 = vpl - 1; 0 <= t1; t1--) {
				prices1[t1] = prices0[t0];
				pv1 += vpd;
				while (pv0 < pv1 && 0 < t0) {
					pv0 += prices0[t0] * vols[t0];
					t0--;
				}
			}

			return prices1;
		}
	}

}
