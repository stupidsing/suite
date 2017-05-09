package suite.trade;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

import suite.streamlet.As;
import suite.streamlet.Read;
import suite.util.Util;

public class Account {

	public static final String cashCode = Asset.cash.code;

	private Map<String, Integer> assets = new HashMap<>();
	private int nTransactions = 0;
	private float nTransactionAmount = 0f;

	public static Account fromHistory(List<Trade> trades) {
		Account account = new Account();
		for (Trade trade : trades)
			account.play(trade);
		return account;
	}

	public static Account fromPortfolio(Predicate<Trade> pred) {
		return fromPortfolio(TradeUtil.fromHistory(pred));
	}

	public static Account fromPortfolio(List<Trade> trades) {
		return new Account(TradeUtil.portfolio(trades));
	}

	public static Account fromCash(float cash) {
		Map<String, Integer> assets = new HashMap<>();
		assets.put(cashCode, (int) cash);
		return new Account(assets);
	}

	private Account() {
		this(new HashMap<>());
	}

	private Account(Map<String, Integer> assets) {
		this.assets = assets;
	}

	public float valuation(Map<String, Float> prices0) {
		Map<String, Float> prices1 = new HashMap<>(prices0);
		prices1.put(cashCode, 1f);
		return (float) Read.from2(assets()) //
				.collect(As.<String, Integer> sumOfDoubles((stockCode, n) -> prices1.get(stockCode) * n));
	}

	public String switchPortfolio(Map<String, Integer> assets1, Map<String, Float> prices) {
		Map<String, Integer> assets0 = assets;
		List<Trade> trades = TradeUtil.diff(assets0, assets1, prices);

		for (Trade trade : trades)
			play(trade);

		return Read.from(trades) //
				.filter(trade -> trade.buySell != 0) //
				.map(Trade::toString) //
				.collect(As.joined());
	}

	public void play(Trade trade) {
		int buySell = trade.buySell;
		String stockCode = trade.stockCode;
		float cost = buySell * trade.price;

		int cash0 = cash();
		int cash1 = (int) (cash0 - cost);
		int nShares0 = nShares(stockCode);
		int nShares1 = nShares0 + buySell;

		update(cashCode, cash1);
		update(stockCode, nShares1);
		if (buySell != 0)
			nTransactions++;
		nTransactionAmount += Math.abs(cost);
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

	public int nShares(String stockCode) {
		return get(stockCode);
	}

	private int get(String code) {
		return assets().computeIfAbsent(code, s -> 0);
	}

	@Override
	public String toString() {
		return TradeUtil.format(assets());
	}

	public Map<String, Integer> assets() {
		return assets;
	}

	public int nTransactions() {
		return nTransactions;
	}

	public float transactionAmount() {
		return nTransactionAmount;
	}

}
