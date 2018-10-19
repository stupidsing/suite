package suite.trade.analysis;

import static suite.util.Friends.forInt;

import org.junit.Test;

import suite.algo.KmeansCluster;
import suite.primitive.IntFunUtil;
import suite.primitive.adt.map.ObjIntMap;
import suite.primitive.adt.pair.IntIntPair;
import suite.trade.data.TradeCfgImpl;

// mvn test -Dtest=AnalyzeTimeSeriesTest#test
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
		var ft = new ObjIntMap<IntIntPair>();

		for (var i = 1; i < kmc.length; i++)
			ft.update(IntIntPair.of(kmc[i - 1], kmc[i]), v0 -> 1 + (v0 != IntFunUtil.EMPTYVALUE ? v0 : 0));

		for (var s0 : forInt(k))
			for (var s1 : forInt(k))
				System.out.println("from = " + s0 + ", to = " + s1 + ", count = " + ft.get(IntIntPair.of(s0, s1)));
	}

}
