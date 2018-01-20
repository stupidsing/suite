package suite.trade;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import suite.primitive.DblPrimitives.ObjObj_Dbl;
import suite.primitive.Dbl_Dbl;
import suite.primitive.FltPrimitives.Obj_Flt;
import suite.streamlet.Read;
import suite.streamlet.Streamlet2;
import suite.util.Fail;
import suite.util.String_;
import suite.util.To;

public class Account {

	private static String cashCode = Asset.cashSymbol;

	public static Account ofCash(float cash) {
		Map<String, Integer> assets = new HashMap<>();
		assets.put(cashCode, (int) cash);
		return new Account(assets);
	}

	public static Account ofHistory(Iterable<Trade> trades) {
		Account account = new Account(new HashMap<>());
		account.play_(trades, false);
		return account;
	}

	public static Account ofPortfolio(Iterable<Trade> trades) {
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

	public boolean play(Trade trade, boolean isValidate) {
		return play_(trade, isValidate);
	}

	public String playValidate(List<Trade> trades) {
		play_(trades, !Trade_.isFreePlay);
		validate();
		return Trade_.format(trades);
	}

	public Streamlet2<String, Integer> portfolio() {
		return Read.from2(assets).filter((symbol, n) -> n != 0);
	}

	@Override
	public String toString() {
		return Trade_.format(assets);
	}

	public float transactionAmount() {
		return transactionAmount;
	}

	public TransactionSummary transactionSummary(Dbl_Dbl transactionFeeFun) {
		return new TransactionSummary(nTransactions, (float) transactionAmount, (float) transactionFeeFun.apply(transactionAmount));
	}

	public class TransactionSummary {
		public final int nTransactions;
		public final float transactionAmount;
		public final float transactionFee;

		public TransactionSummary(int nTransactions, float transactionAmount, float transactionFee) {
			this.nTransactions = nTransactions;
			this.transactionAmount = transactionAmount;
			this.transactionFee = transactionFee;
		}

		public String out0() {
			return "txnFee:" + To.string(transactionFee) + "/" + nTransactions;
		}

		public String out1() {
			return "transactions = " + To.string(transactionAmount) + "/" + nTransactions + ", fee = " + To.string(transactionFee);
		}
	}

	private void update(String code, int amount) {
		if (amount != 0)
			assets.put(code, amount);
		else
			assets.remove(code);
	}

	public void validate() {
		int cash = cash_();
		if (!Trade_.isValidCash(cash))
			Fail.t("too much leverage: " + cash);
		assets.forEach((symbol, nShares) -> {
			if (!Trade_.isValidStock(symbol, nShares))
				Fail.t("no short-selling " + symbol + " " + nShares);
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

		public Streamlet2<String, Float> streamlet() {
			return Read.from2(valuationBySymbol);
		}

		public float sum() {
			return (float) streamlet().toDouble(ObjObj_Dbl.sum((symbol, v) -> v));
		}
	}

	private int cash_() {
		return get(cashCode);
	}

	private boolean play_(Iterable<Trade> trades, boolean isValidate) {
		boolean result = true;
		for (Trade trade : trades)
			result &= play_(trade, isValidate);
		return result;
	}

	private boolean play_(Trade trade, boolean isValidate) {
		float price = trade.price;

		if (Trade_.negligible < price && price < Trade_.max) {
			String symbol = trade.symbol;
			int buySell = trade.buySell;
			float cost = trade.amount();

			int cash0 = get(cashCode);
			int cash1 = (int) (cash0 - cost);
			int nShares0 = get(symbol);
			int nShares1 = nShares0 + buySell;
			boolean isPlayable = !isValidate || Trade_.isValidCash(cash1) && Trade_.isValidStock(symbol, nShares1);

			if (isPlayable) {
				update(cashCode, cash1);
				update(symbol, nShares1);
				if (buySell != 0)
					nTransactions++;
				transactionAmount += Math.abs(cost);
			}

			return isPlayable;
		} else
			return Fail.t("impossible transaction price: " + trade);
	}

	private int get(String code) {
		return assets.computeIfAbsent(code, s -> 0);
	}

}
