package suite.trade.analysis;

import static suite.util.Friends.forInt;

import org.junit.Test;

import suite.algo.KmeansCluster;
import suite.trade.data.TradeCfgImpl;

public class CandlestickPatternTest {

	private TradeCfgImpl cfg = new TradeCfgImpl();

	@Test
	public void test() {
		var ds = cfg.dataSource("0005.HK");
		var k = 6;

		var vectors = forInt(ds.ts.length).map(t -> {
			var price = ds.prices[t];
			var invPrice = 1d / price;
			var o = (float) (ds.opens[t] * invPrice);
			var c = (float) (ds.closes[t] * invPrice);
			var l = (float) (ds.lows[t] * invPrice);
			var h = (float) (ds.highs[t] * invPrice);
			var v = ds.volumes[t];
			return new float[] { o, c, l, h, v, };
		}).toList();

		var kmc = new KmeansCluster(5).kMeansCluster(vectors, k, 99);
		var hist = new int[k];
		var ft = new int[k][k];

		for (var c : kmc)
			hist[c]++;

		for (var i = 1; i < kmc.length; i++)
			ft[kmc[i - 1]][kmc[i]]++;

		for (var i : forInt(k))
			System.out.println("count " + i + " = " + hist[i]);

		for (var s0 : forInt(k))
			for (var s1 : forInt(k))
				System.out.println("from = " + s0 + ", to = " + s1 + ", count = " + ft[s0][s1]);
	}

}
