package suite.trade;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

import suite.adt.Pair;
import suite.streamlet.As;
import suite.streamlet.Read;
import suite.trade.Trans.Record;
import suite.util.Util;

public class Account {

	public static final String cashCode = Asset.cash.code;
	public static final String defaultStockCode = "-";

	private Map<String, Integer> assets = new HashMap<>();
	private int nTransactions = 0;

	public static Account fromHistory(Predicate<Record> pred) {
		return fromHistory(Trans.fromHistory(pred));
	}

	public static Account fromHistory(List<Record> table) {
		return new Account(Trans.portfolio(table));
	}

	public static Account fromCash(float cash) {
		Map<String, Integer> assets = new HashMap<>();
		assets.put(cashCode, (int) cash);
		return new Account(assets);
	}

	public Account() {
		this(new HashMap<>());
	}

	private Account(Map<String, Integer> assets) {
		this.assets = assets;
	}

	public float valuation(Map<String, Float> prices0) {
		Map<String, Float> prices1 = new HashMap<>(prices0);
		prices1.put(cashCode, 1f);
		return Read.from2(assets()) //
				.collect(As.<String, Integer> sumOfFloats((stockCode, n) -> prices1.get(stockCode) * n));
	}

	public String portfolio(Map<String, Integer> assets1, Map<String, Float> prices) {
		Map<String, Integer> assets0 = assets;

		List<Pair<String, Integer>> buySells = Trans.diff(assets0, assets1);

		for (Pair<String, Integer> buySell : buySells) {
			String stockCode = buySell.t0;
			buySell(stockCode, buySell.t1, prices.get(stockCode));
		}

		return Read.from2(buySells) //
				.filterValue(n -> n != 0) //
				.map((stockCode, n) -> "" + stockCode + ":" + prices.get(stockCode) + "*" + n + ",") //
				.collect(As.joined());
	}

	public void buySell(int buySell, float price) {
		buySell(defaultStockCode, buySell, price);
	}

	public void buySell(String stockCode, int buySell, float price) {
		int cash0 = cash();
		int cash1 = (int) (cash0 - buySell * price);
		int nShares0 = nShares(stockCode);
		int nShares1 = nShares0 + buySell;
		update(cashCode, cash1);
		update(stockCode, nShares1);
		nTransactions += Math.abs(buySell);
	}

	private void update(String code, int amount) {
		if (amount != 0)
			assets.put(code, amount);
		else
			assets.remove(code);
	}

	public void validate() {
		assets.forEach((code, nShares) -> {
			if (!Util.stringEquals(code, cashCode) && nShares < 0)
				throw new RuntimeException("no short-selling " + nShares + " shares for " + code);
		});
	}

	public int cash() {
		return get(cashCode);
	}

	public int nShares() {
		return nShares(defaultStockCode);
	}

	public int nShares(String stockCode) {
		return get(stockCode);
	}

	private int get(String code) {
		return assets().computeIfAbsent(code, s -> 0);
	}

	public Map<String, Integer> assets() {
		return assets;
	}

	public int nTransactions() {
		return nTransactions;
	}

}
