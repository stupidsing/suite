package suite.trade;

import static java.lang.Math.abs;
import static primal.statics.Fail.fail;
import static primal.statics.Fail.failBool;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import primal.MoreVerbs.Read;
import primal.Verbs.Equals;
import primal.primitive.Dbl_Dbl;
import primal.primitive.FltPrim.Obj_Flt;
import primal.primitive.fp.AsDbl;
import primal.streamlet.Streamlet2;
import suite.util.To;

public class Account {

	private static String cashCode = Instrument.cashSymbol;

	public static Account ofCash(float cash) {
		var instruments = new HashMap<String, Integer>();
		instruments.put(cashCode, (int) cash);
		return new Account(instruments);
	}

	public static Account ofHistory(Iterable<Trade> trades) {
		var account = new Account(new HashMap<>());
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
			return "txFee:" + To.string(transactionFee) + "/" + nTransactions;
		}

		public String out1() {
			return "FEE = " + To.string(-transactionFee);
		}
	}

	private void update(String symbol, int amount) {
		if (amount != 0)
			assets.put(symbol, amount);
		else
			assets.remove(symbol);
	}

	public boolean validate() {
		var cash = cash_();
		return (Trade_.isValidCash(cash) || failBool("too much leverage: " + cash)) //
				&& (Read.from2(assets).isAll((symbol, nShares) -> {
					return Trade_.isValidStock(symbol, nShares) || failBool("no short-selling " + symbol + " " + nShares);
				}));
	}

	public Valuation valuation(Obj_Flt<String> priceFun) {
		return new Valuation(priceFun);
	}

	public class Valuation {
		public final Map<String, Float> valuationBySymbol;

		private Valuation(Obj_Flt<String> priceFun0) {
			Obj_Flt<String> priceFun1 = symbol -> !Equals.string(symbol, cashCode) ? priceFun0.apply(symbol) : 1f;
			valuationBySymbol = Read.from2(assets).map2((symbol, n) -> priceFun1.apply(symbol) * n).toMap();
		}

		public Streamlet2<String, Float> streamlet() {
			return Read.from2(valuationBySymbol);
		}

		public float sum() {
			return (float) streamlet().toDouble(AsDbl.sum((symbol, v) -> v));
		}
	}

	private int cash_() {
		return get(cashCode);
	}

	private boolean play_(Iterable<Trade> trades, boolean isValidate) {
		var b = true;
		for (var trade : trades)
			b &= play_(trade, isValidate);
		return b;
	}

	private boolean play_(Trade trade, boolean isValidate) {
		var price = trade.price;

		if (0f <= price && price < Trade_.max) {
			var symbol = trade.symbol;
			var buySell = trade.buySell;
			var cost = trade.amount();

			var cash0 = get(cashCode);
			var cash1 = (int) (cash0 - cost);
			var nShares0 = get(symbol);
			var nShares1 = nShares0 + buySell;
			var isPlayable = !isValidate || Trade_.isValidCash(cash1) && Trade_.isValidStock(symbol, nShares1);

			if (isPlayable) {
				update(cashCode, cash1);
				update(symbol, nShares1);
				if (buySell != 0)
					nTransactions++;
				transactionAmount += abs(cost);
			}

			return isPlayable;
		} else
			return fail("impossible transaction price: " + trade);
	}

	private int get(String code) {
		return assets.computeIfAbsent(code, s -> 0);
	}

}
