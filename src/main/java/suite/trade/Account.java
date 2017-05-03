package suite.trade;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import suite.adt.Pair;
import suite.streamlet.Read;
import suite.streamlet.Streamlet2;

public class Account {

	private static String defaultStockCode = "-";

	private float cash;
	private Map<String, Integer> assets = new HashMap<>();
	private int nTransactions = 0;

	public Account() {
		this(0f);
	}

	public Account(float cash) {
		this.cash = cash;
	}

	public void portfolio(Map<String, Integer> assets1, Map<String, Float> prices) {
		Map<String, Integer> assets0 = assets;

		Set<String> stockCodes = Streamlet2.concat(Read.from2(assets0), Read.from2(assets1)) //
				.map((stockCode, nShares) -> stockCode) //
				.toSet();

		List<Pair<String, Integer>> buySells = Read.from(stockCodes) //
				.map2(stockCode -> {
					int n0 = assets0.computeIfAbsent(stockCode, s -> 0);
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
		int nShares0 = nShares(stockCode);
		int nShares1 = nShares0 + buySell;
		if (nShares1 != 0)
			assets.put(stockCode, nShares1);
		else
			assets.remove(stockCode);
		nTransactions += Math.abs(buySell);
	}

	public void validate() {
		if (cash < 0)
			throw new RuntimeException("invalid condition");
		assets.forEach((stockCode, buySell) -> {
			if (buySell < 0)
				throw new RuntimeException("invalid condition");
		});
	}

	public float cash() {
		return cash;
	}

	public int nShares() {
		return nShares(defaultStockCode);
	}

	public int nShares(String stockCode) {
		return assets().computeIfAbsent(stockCode, s -> 0);
	}

	public Map<String, Integer> assets() {
		return assets;
	}

	public int nTransactions() {
		return nTransactions;
	}

}
