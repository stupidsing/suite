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
		List<Trade> table0 = TradeUtil.fromHistory(trade -> true);
		Map<String, Float> priceBySymbol = cfg.quote(Read.from(table0).map(trade -> trade.symbol).toSet());
		List<Trade> table1 = To.list(table0, sellAll(table0, priceBySymbol));

		Map<K, String> constituentsByStrategy = Read.from(table0) //
				.groupBy(fun) //
				.mapValue(table -> Read.from2(TradeUtil.portfolio(table)) //
						.map((symbol, nShares) -> {
							Asset asset = cfg.getCompany(symbol);
							float price = priceBySymbol.get(symbol);
							return "\n" + asset + ": " + price + " * " + nShares + " = " + nShares * price;
						}) //
						.collect(As.joined())) //
				.toMap();

		Account account0 = Account.fromHistory(table0);
		Account account1 = Account.fromHistory(table1);

		double amount0 = account0.cash();
		double amount1 = account1.cash();
		double transactionAmount = account0.transactionAmount();

		for (Entry<K, String> e : constituentsByStrategy.entrySet())
			log.accept("CONSTITUENTS (" + e.getKey() + "):" + e.getValue());
		log.accept("OWN = " + -amount0);
		log.accept("P/L = " + amount1);
		log.accept("nTransactions = " + account0.nTransactions());
		log.accept("transactionAmount = " + transactionAmount);
		log.accept("transactionFee = " + To.string(cfg.transactionFee(transactionAmount)));

		return Read.from(table1).groupBy(fun, this::returns).toMap();
	}

	private List<Trade> sellAll(List<Trade> table0, Map<String, Float> priceBySymbol) {
		List<Trade> sellAll = Read.from(table0) //
				.groupBy(trade -> trade.strategy, TradeUtil::portfolio) //
				.concatMap((strategy, nSharesBySymbol) -> Read //
						.from2(nSharesBySymbol) //
						.map((symbol, size) -> new Trade(-size, symbol, priceBySymbol.get(symbol), strategy))) //
				.toList();
		return sellAll;
	}

	// Profit & loss
	private double returns(Iterable<Trade> trades) {
		return Account.fromHistory(trades).cash();
	}

}
