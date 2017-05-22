package suite.trade;

import java.util.HashMap;
import java.util.Map;

import suite.primitive.PrimitiveFun.Double_Double;
import suite.streamlet.As;
import suite.streamlet.Read;
import suite.streamlet.Streamlet2;
import suite.util.String_;
import suite.util.To;

public class Account {

	private static String cashCode = Asset.cashCode;

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
		return new Account(Trade_.portfolio(trades));
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
		return cash_();
	}

	public int nShares(String symbol) {
		return get(symbol);
	}

	public void play(Iterable<Trade> trades) {
		for (Trade trade : trades)
			play_(trade);
	}

	public void play(Trade trade) {
		play_(trade);
	}

	@Override
	public String toString() {
		return Trade_.format(assets());
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
		if (cash_() < -Trade_.maxLeverageAmount)
			throw new RuntimeException("too much leverage: " + cash_());
		assets.forEach((symbol, nShares) -> {
			if (!Trade_.isShortSell && !String_.equals(symbol, cashCode) && nShares < 0)
				throw new RuntimeException("no short-selling " + symbol + " " + nShares);
		});
	}

	public Valuation valuation(Map<String, Float> prices0) {
		return new Valuation(prices0);
	}

	public class Valuation {
		public final Map<String, Float> valuationBySymbol;

		private Valuation(Map<String, Float> prices0) {
			Map<String, Float> prices1 = new HashMap<>(prices0);
			prices1.put(cashCode, 1f);

			valuationBySymbol = Read.from2(assets).map2((symbol, n) -> prices1.get(symbol) * n).toMap();
		}

		public Streamlet2<String, Float> stream() {
			return Read.from2(valuationBySymbol);
		}

		public float sum() {
			return (float) Read.from2(valuationBySymbol).collectAsDouble(As.sumOfDoubles((symbol, v) -> v));
		}
	}

	private int cash_() {
		return get(cashCode);
	}

	private void play_(Trade trade) {
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

	private int get(String code) {
		return assets.computeIfAbsent(code, s -> 0);
	}

}
