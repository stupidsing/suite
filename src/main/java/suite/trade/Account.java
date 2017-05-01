package suite.trade;

import suite.adt.ObjIntMap;

public class Account {

	private static String defaultStockCode = "-";

	private float cash = 0;
	private ObjIntMap<String> nLots = new ObjIntMap<>();
	private int nTransactions = 0;

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
		return nLots.get(stockCode);
	}

	public int nTransactions() {
		return nTransactions;
	}

}
