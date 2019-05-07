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

	private void analyze(DataSource ds, int pvl) {
		var vpt = new VolumePriceTick(ds.closes, ds.volumes, pvl);
		var prices = Boolean.TRUE ? vpt.forward() : vpt.backward();

		for (var days = 1; days < 9; days++) {
			var ma_ = ma.movingAvg(prices, days);
			var returns = cr.buySell(t -> prices[t] < ma_[t] ? -1d : 1d).invest(prices);
			System.out.println("ma[" + days + "] = " + returns.sharpe());
		}
	}

	private class VolumePriceTick {
		private float[] prices;
		private float[] vols;
		private int pvl;
		private double pvd;

		private VolumePriceTick(float[] prices, float[] vols, int pvl) {
			this.prices = prices;
			this.vols = vols;
			this.pvl = pvl;

			pvd = vec.dot(prices, vols) / pvl;
		}

		private float[] forward() {
			var prices0 = prices;
			var prices1 = new float[pvl];
			var pv0 = 0d;
			var pv1 = 0d;
			var t0 = 0;

			for (var t1 = 0; t1 < pvl; t1++) {
				prices1[t1] = prices0[t0];
				pv1 += pvd;
				while (pv0 < pv1 && t0 < prices.length) {
					pv0 += prices0[t0] * vols[t0];
					t0++;
				}
			}

			return prices1;
		}

		private float[] backward() {
			var prices0 = prices;
			var prices1 = new float[pvl];
			var pv0 = 0d;
			var pv1 = 0d;
			var t0 = prices.length - 1;

			for (var t1 = pvl - 1; 0 <= t1; t1--) {
				prices1[t1] = prices0[t0];
				pv1 += pvd;
				while (pv0 < pv1 && 0 < t0) {
					pv0 += prices0[t0] * vols[t0];
					t0--;
				}
			}

			return prices1;
		}
	}

}
