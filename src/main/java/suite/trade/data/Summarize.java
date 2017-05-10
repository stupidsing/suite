package suite.trade.data;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Consumer;

import suite.Constants;
import suite.streamlet.As;
import suite.streamlet.Read;
import suite.trade.Account;
import suite.trade.Asset;
import suite.trade.Trade;
import suite.trade.TradeUtil;
import suite.util.FunUtil.Fun;
import suite.util.To;

public class Summarize {

	private Configuration cfg;

	public Summarize(Configuration cfg) {
		this.cfg = cfg;
	}

	public <K> Map<K, Double> summarize(Fun<Trade, K> fun, Consumer<String> log) {
		List<Trade> trades = TradeUtil.fromHistory(trade -> true);
		Map<String, Float> priceBySymbol = cfg.quote(Read.from(trades).map(trade -> trade.symbol).toSet());

		Map<K, String> summaryByKey = Read.from(trades) //
				.groupBy(fun) //
				.mapValue(trades_ -> summarize(trades_, priceBySymbol)) //
				.toMap();

		for (Entry<K, String> e : summaryByKey.entrySet()) {
			K key = e.getKey();
			String summary = e.getValue();
			log.accept(Constants.separator);
			log.accept("For strategy " + key + ":" + summary);
		}

		log.accept(Constants.separator);
		log.accept("Overall:" + summarize(trades, priceBySymbol));

		return Read.from(sellAll(trades, priceBySymbol)) // profit & loss
				.groupBy(fun, t -> (double) Account.fromHistory(t).cash()) //
				.toMap();
	}

	private String summarize(List<Trade> trades_, Map<String, Float> priceBySymbol) {
		List<Trade> trades0 = trades_;
		List<Trade> trades1 = sellAll(trades0, priceBySymbol);
		Account account0 = Account.fromHistory(trades0);
		Account account1 = Account.fromHistory(trades1);
		double amount0 = account0.cash();
		double amount1 = account1.cash();
		double transactionAmount = account0.transactionAmount();

		return Read.from2(TradeUtil.portfolio(trades0)) //
				.map((symbol, nShares) -> {
					Asset asset = cfg.getCompany(symbol);
					float price = priceBySymbol.get(symbol);
					return asset + ": " + price + " * " + nShares + " = " + nShares * price;
				}) //
				.append("OWN = " + -amount0) //
				.append("P/L = " + amount1) //
				.append("nTransactions = " + account0.nTransactions()) //
				.append("transactionAmount = " + transactionAmount) //
				.append("transactionFee = " + To.string(cfg.transactionFee(transactionAmount))) //
				.map(m -> "\n" + m) //
				.collect(As.joined());
	}

	private List<Trade> sellAll(List<Trade> trades, Map<String, Float> priceBySymbol) {
		List<Trade> sellAll = Read.from(trades) //
				.groupBy(trade -> trade.strategy, TradeUtil::portfolio) //
				.concatMap((strategy, nSharesBySymbol) -> Read //
						.from2(nSharesBySymbol) //
						.map((symbol, size) -> new Trade(-size, symbol, priceBySymbol.get(symbol), strategy))) //
				.toList();

		return To.list(trades, sellAll);
	}

}
