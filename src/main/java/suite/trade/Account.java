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
	private ObjIntMap<String> nLots = new ObjIntMap<>();
	private int nTransactions = 0;

	public void portfolio(ObjIntMap<String> nLots1, Map<String, Float> prices) {
		Set<String> stockCodes = IntObjStreamlet.concat(nLots.stream(), nLots1.stream()) //
				.map((nLots, stockCode) -> stockCode) //
				.toSet();

		List<Pair<String, Integer>> buySells = Read.from(stockCodes) //
				.map2(stockCode -> {
					int n0 = nLots.computeIfAbsent(stockCode, s -> 0);
					int n1 = nLots1.computeIfAbsent(stockCode, s -> 0);
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
		nLots.computeIfAbsent(stockCode, s -> 0);
		nLots.update(stockCode, lot -> lot + buySell);
		nTransactions += Math.abs(buySell);
	}

	public void validate() {
		if (cash < 0)
			throw new RuntimeException("invalid condition");
		nLots.forEach((buySell, stockCode) -> {
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
		return nLots.computeIfAbsent(stockCode, s -> 0);
	}

	public int nTransactions() {
		return nTransactions;
	}

}
