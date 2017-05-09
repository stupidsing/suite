package suite.trade.data;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Consumer;

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

		Map<K, List<String>> messagesByKey = Read.from(trades) //
				.groupBy(fun) //
				.mapValue(trades_ -> {
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
							.toList();
				}) //
				.toMap();

		for (Entry<K, List<String>> e : messagesByKey.entrySet()) {
			K key = e.getKey();
			List<String> messages = e.getValue();
			log.accept("For strategy " + key + ":" + Read.from(messages).collect(As.conc("\n")));
		}

		return Read.from(sellAll(trades, priceBySymbol)).groupBy(fun, this::returns).toMap();
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

	// Profit & loss
	private double returns(Iterable<Trade> trades) {
		return Account.fromHistory(trades).cash();
	}

}
