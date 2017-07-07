package suite.trade;

import java.util.HashMap;
import java.util.Map;

import suite.primitive.DblPrimitives.ObjObj_Dbl;
import suite.primitive.Dbl_Dbl;
import suite.primitive.FltPrimitives.Obj_Flt;
import suite.streamlet.Read;
import suite.streamlet.Streamlet2;
import suite.util.String_;
import suite.util.To;

public class Account {

	private static String cashCode = Asset.cashSymbol;

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

	public String transactionSummary(Dbl_Dbl transactionFeeFun) {
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
		if (cash_() < -Trade_.leverageAmount)
			throw new RuntimeException("too much leverage: " + cash_());
		assets.forEach((symbol, nShares) -> {
			if (!Trade_.isShortSell && !String_.equals(symbol, cashCode) && nShares < 0)
				throw new RuntimeException("no short-selling " + symbol + " " + nShares);
		});
	}

	public Valuation valuation(Obj_Flt<String> priceFun) {
		return new Valuation(priceFun);
	}

	public class Valuation {
		public final Map<String, Float> valuationBySymbol;

		private Valuation(Obj_Flt<String> priceFun0) {
			Obj_Flt<String> priceFun1 = symbol -> !String_.equals(symbol, cashCode) ? priceFun0.apply(symbol) : 1f;
			valuationBySymbol = Read.from2(assets).map2((symbol, n) -> priceFun1.apply(symbol) * n).toMap();
		}

		public Streamlet2<String, Float> stream() {
			return Read.from2(valuationBySymbol);
		}

		public float sum() {
			return (float) Read.from2(valuationBySymbol).collectAsDouble(ObjObj_Dbl.sum((symbol, v) -> v));
		}
	}

	private int cash_() {
		return get(cashCode);
	}

	private void play_(Trade trade) {
		float price = trade.price;

		if (Trade_.negligible < price && price < Trade_.max) {
			int buySell = trade.buySell;
			String symbol = trade.symbol;
			float cost = buySell * price;

			int cash0 = get(cashCode);
			int cash1 = (int) (cash0 - cost);
			int nShares0 = get(symbol);
			int nShares1 = nShares0 + buySell;

			update(cashCode, cash1);
			update(symbol, nShares1);
			if (buySell != 0)
				nTransactions++;
			transactionAmount += Math.abs(cost);
		} else
			throw new RuntimeException("impossible transaction price: " + trade);
	}

	private int get(String code) {
		return assets.computeIfAbsent(code, s -> 0);
	}

}
