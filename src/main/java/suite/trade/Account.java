package suite.trade;

import java.util.List;
import java.util.Map;
import java.util.Set;

import suite.adt.ObjIntMap;
import suite.adt.Pair;
import suite.streamlet.IntObjStreamlet;
import suite.streamlet.Read;

public class Account {

	private static String defaultStockCode = "-";

	private float cash = 0;
	private ObjIntMap<String> assets = new ObjIntMap<>();
	private int nTransactions = 0;

	public void portfolio(ObjIntMap<String> assets1, Map<String, Float> prices) {
		Set<String> stockCodes = IntObjStreamlet.concat(assets.stream(), assets1.stream()) //
				.map((nLots, stockCode) -> stockCode) //
				.toSet();

		List<Pair<String, Integer>> buySells = Read.from(stockCodes) //
				.map2(stockCode -> {
					int n0 = assets.computeIfAbsent(stockCode, s -> 0);
					int n1 = assets1.computeIfAbsent(stockCode, s -> 0);
					return n1 - n0;
				}) //
				.toList();

		for (Pair<String, Integer> buySell : buySells) {
			String stockCode = buySell.t0;
			buySell(stockCode, buySell.t1, prices.get(stockCode));
		}
	}

	public void buySell(int buySell, float price) {
		buySell(defaultStockCode, buySell, price);
	}

	public void buySell(String stockCode, int buySell, float price) {
		cash -= buySell * price;
		assets.computeIfAbsent(stockCode, s -> 0);
		assets.update(stockCode, lot -> lot + buySell);
		nTransactions += Math.abs(buySell);
	}

	public void validate() {
		if (cash < 0)
			throw new RuntimeException("invalid condition");
		assets.forEach((buySell, stockCode) -> {
			if (buySell < 0)
				throw new RuntimeException("invalid condition");
		});
	}

	public float cash() {
		return cash;
	}

	public int nLots() {
		return nLots(defaultStockCode);
	}

	public int nLots(String stockCode) {
		return assets.computeIfAbsent(stockCode, s -> 0);
	}

	public int nTransactions() {
		return nTransactions;
	}

}
