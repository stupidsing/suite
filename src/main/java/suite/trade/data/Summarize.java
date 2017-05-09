package suite.trade.data;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

import suite.streamlet.Read;
import suite.streamlet.Streamlet;
import suite.trade.Account;
import suite.trade.Asset;
import suite.trade.Trade;
import suite.trade.TradeUtil;
import suite.util.FunUtil.Fun;
import suite.util.To;

public class Summarize {

	private Configuration configuration;

	public Summarize(Configuration configuration) {
		this.configuration = configuration;
	}

	public <K> Map<K, Double> summarize(Fun<Trade, K> fun, Consumer<String> log) {
		List<Trade> table0 = TradeUtil.fromHistory(trade -> true);
		Map<String, Integer> nSharesBySymbols = TradeUtil.portfolio(table0);
		Set<String> symbols = nSharesBySymbols.keySet();
		Map<String, Float> priceBySymbols = configuration.quote(symbols);

		List<Trade> sellAll = Read.from(table0) //
				.groupBy(trade -> trade.strategy, st -> TradeUtil.portfolio(st.toList())) //
				.concatMap((strategy, nSharesBySymbol) -> Read //
						.from2(nSharesBySymbol) //
						.map((symbol, size) -> {
							float price = priceBySymbols.get(symbol);
							return new Trade(-size, symbol, price, strategy);
						})) //
				.toList();

		List<Trade> table1 = To.list(table0, sellAll);

		Account account0 = Account.fromHistory(table0);
		Account account1 = Account.fromHistory(table1);
		double amount0 = account0.cash();
		double amount1 = account1.cash();

		Streamlet<String> constituents = Read.from2(nSharesBySymbols) //
				.map((symbol, nShares) -> {
					Asset asset = configuration.getCompany(symbol);
					float price = priceBySymbols.get(symbol);
					return asset + ": " + price + " * " + nShares + " = " + nShares * price;
				});

		int nTransactions = account0.nTransactions();
		double transactionAmount = account0.transactionAmount();

		log.accept("CONSTITUENTS:");
		constituents.forEach(log);
		log.accept("OWN = " + -amount0);
		log.accept("P/L = " + amount1);
		log.accept("nTransactions = " + nTransactions);
		log.accept("transactionAmount = " + transactionAmount);
		log.accept("transactionFee = " + To.string(configuration.transactionFee(transactionAmount)));

		return Read.from(table1) //
				.groupBy(fun, st -> returns(st.toList())) //
				.toMap();
	}

	// Profit & loss
	private double returns(List<Trade> trades) {
		return Account.fromHistory(trades).cash();
	}

}
