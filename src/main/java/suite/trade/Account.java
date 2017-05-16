package suite.trade;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import suite.primitive.PrimitiveFun.Double_Double;
import suite.streamlet.As;
import suite.streamlet.Read;
import suite.util.String_;
import suite.util.To;

public class Account {

	public static final String cashCode = Asset.cashCode;

	public static Account fromCash(float cash) {
		Map<String, Integer> assets = new HashMap<>();
		assets.put(cashCode, (int) cash);
		return new Account(assets);
	}

	public static Account fromHistory(Iterable<Trade> trades) {
		Account account = new Account(new HashMap<>());
		account.play(trades);
		return account;
	}

	public static Account fromPortfolio(Iterable<Trade> trades) {
		return new Account(TradeUtil.portfolio(trades));
	}

	private Map<String, Integer> assets = new HashMap<>();
	private int nTransactions = 0;
	private float transactionAmount = 0f;

	private Account(Map<String, Integer> assets) {
		this.assets = assets;
	}

	public Map<String, Integer> assets() {
		return assets;
	}

	public int cash() {
		return get(cashCode);
	}

	public int nShares(String symbol) {
		return get(symbol);
	}

	private void play(Iterable<Trade> trades) {
		for (Trade trade : trades)
			play(trade);
	}

	public void play(Trade trade) {
		int buySell = trade.buySell;
		String symbol = trade.symbol;
		float cost = buySell * trade.price;

		int cash0 = get(cashCode);
		int cash1 = (int) (cash0 - cost);
		int nShares0 = get(symbol);
		int nShares1 = nShares0 + buySell;

		update(cashCode, cash1);
		update(symbol, nShares1);
		if (buySell != 0)
			nTransactions++;
		transactionAmount += Math.abs(cost);
	}

	public String switchPortfolio(Map<String, Integer> assets1, Map<String, Float> prices) {
		Map<String, Integer> assets0 = assets;
		List<Trade> trades = TradeUtil.diff(assets0, assets1, prices);

		play(trades);

		return Read.from(trades) //
				.filter(trade -> trade.buySell != 0) //
				.map(Trade::toString) //
				.collect(As.joined());
	}

	@Override
	public String toString() {
		return TradeUtil.format(assets());
	}

	public float transactionAmount() {
		return transactionAmount;
	}

	public String transactionSummary(Double_Double transactionFeeFun) {
		double fee = transactionFeeFun.apply(transactionAmount);
		return "transactions = " + To.string(transactionAmount) + "/" + nTransactions + ", fee = " + To.string(fee);
	}

	private void update(String code, int amount) {
		if (amount != 0)
			assets.put(code, amount);
		else
			assets.remove(code);
	}

	public void validate() {
		assets.forEach((code, nShares) -> {
			if (!String_.equals(code, cashCode) && nShares < 0)
				throw new RuntimeException("no short-selling " + nShares + " shares for " + code);
		});
	}

	public float valuation(Map<String, Float> prices0) {
		return (float) Read.from2(valuationBySymbol(prices0)) //
				.collectAsDouble(As.<String, Float> sumOfDoubles((symbol, v) -> v));
	}

	public Map<String, Float> valuationBySymbol(Map<String, Float> prices0) {
		Map<String, Float> prices1 = new HashMap<>(prices0);
		prices1.put(cashCode, 1f);
		return Read.from2(assets()) //
				.map2((symbol, n) -> symbol, (symbol, n) -> prices1.get(symbol) * n) //
				.toMap();
	}

	private int get(String code) {
		return assets.computeIfAbsent(code, s -> 0);
	}

}
