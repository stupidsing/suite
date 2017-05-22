package suite.trade.data;

import java.util.Map;
import java.util.Map.Entry;

import suite.Constants;
import suite.streamlet.As;
import suite.streamlet.Read;
import suite.streamlet.Streamlet;
import suite.trade.Account;
import suite.trade.Asset;
import suite.trade.Trade;
import suite.trade.Trade_;
import suite.util.FunUtil.Fun;
import suite.util.FunUtil.Sink;
import suite.util.To;

public class Summarize {

	private Configuration cfg;
	private Streamlet<Trade> trades;
	private Map<String, Float> priceBySymbol;

	public static Summarize of(Configuration cfg) {
		return of(cfg, cfg.queryHistory());
	}

	public static Summarize of(Configuration cfg, Streamlet<Trade> trades) {
		Map<String, Float> priceBySymbol = cfg.quote(trades.map(trade -> trade.symbol).toSet());
		return new Summarize(cfg, trades, priceBySymbol);
	}

	private Summarize(Configuration cfg, Streamlet<Trade> trades, Map<String, Float> priceBySymbol) {
		this.cfg = cfg;
		this.trades = trades;
		this.priceBySymbol = priceBySymbol;
	}

	public <K> Map<K, Double> out(Sink<String> log) {
		return out(log, trade -> null);
	}

	public <K> Map<K, Double> out(Sink<String> log, Fun<Trade, K> fun) {
		Map<K, String> summaryByKey = trades //
				.groupBy(fun) //
				.filterKey(key -> key != null) //
				.mapValue(trades_ -> summarize(Read.from(trades_), priceBySymbol)) //
				.toMap();

		for (Entry<K, String> e : summaryByKey.entrySet()) {
			K key = e.getKey();
			String summary = e.getValue();
			log.sink("\nFor strategy " + key + ":" + summary);
		}

		log.sink(Constants.separator);
		log.sink("Overall:" + summarize(trades, priceBySymbol));

		return sellAll(trades, priceBySymbol) // profit & loss
				.groupBy(fun, t -> (double) Account.fromHistory(t).cash()) //
				.toMap();
	}

	private String summarize(Streamlet<Trade> trades_, Map<String, Float> priceBySymbol) {
		Streamlet<Trade> trades0 = trades_;
		Streamlet<Trade> trades1 = sellAll(trades0, priceBySymbol);
		Account account0 = Account.fromHistory(trades0);
		Account account1 = Account.fromHistory(trades1);
		double amount0 = account0.cash();
		double amount1 = account1.cash();

		return Read.from2(Trade_.portfolio(trades0)) //
				.map((symbol, nShares) -> {
					Asset asset = cfg.queryCompany(symbol);
					float price = priceBySymbol.get(symbol);
					return asset + ": " + price + " * " + nShares + " = " + To.string(nShares * price);
				}) //
				.append("OWN = " + -amount0) //
				.append("P/L = " + amount1) //
				.append(account0.transactionSummary(cfg::transactionFee)) //
				.map(m -> "\n" + m) //
				.collect(As.joined());
	}

	private Streamlet<Trade> sellAll(Streamlet<Trade> trades, Map<String, Float> priceBySymbol) {
		return Streamlet.concat(trades, Trade_.sellAll(trades, priceBySymbol::get));
	}

}
