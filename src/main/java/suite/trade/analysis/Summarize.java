package suite.trade.analysis;

import java.util.Map;
import java.util.Map.Entry;

import suite.streamlet.As;
import suite.streamlet.Read;
import suite.streamlet.Streamlet;
import suite.trade.Account;
import suite.trade.Asset;
import suite.trade.Trade;
import suite.trade.Trade_;
import suite.trade.data.Configuration;
import suite.util.FunUtil.Fun;
import suite.util.FunUtil.Sink;
import suite.util.Object_;
import suite.util.To;

public class Summarize {

	private Configuration cfg;

	public final Streamlet<Trade> trades;
	public final Map<String, Float> priceBySymbol;

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

	public SummarizeByStrategy<Object> summarize() {
		return summarize(trade -> null);
	}

	public <K> SummarizeByStrategy<K> summarize(Fun<Trade, K> fun) {
		StringBuilder sb = new StringBuilder();
		Sink<String> log = sb::append;

		Map<K, String> summaryByKey = trades //
				.groupBy(fun) //
				.filterKey(key -> key != null) //
				.mapValue(trades_ -> summarize_(Read.from(trades_), priceBySymbol)) //
				.toMap();

		for (Entry<K, String> e : summaryByKey.entrySet()) {
			K key = e.getKey();
			String summary = e.getValue();
			log.sink("\nFor strategy " + key + ":" + summary);
		}

		log.sink("Overall:" + summarize_(trades, priceBySymbol));

		// profit and loss
		Map<K, Double> pnlByKey = sellAll(trades, priceBySymbol) //
				.groupBy(fun, t -> (double) Account.ofHistory(t).cash()) //
				.toMap();

		return new SummarizeByStrategy<>(sb.toString(), pnlByKey);
	}

	public class SummarizeByStrategy<K> {
		public final String log;
		public final Map<K, Double> pnlByKey;

		private SummarizeByStrategy(String log, Map<K, Double> pnlBySymbol) {
			this.log = log;
			this.pnlByKey = pnlBySymbol;
		}
	}

	private String summarize_(Streamlet<Trade> trades_, Map<String, Float> priceBySymbol) {
		Streamlet<Trade> trades0 = trades_;
		Streamlet<Trade> trades1 = sellAll(trades0, priceBySymbol);
		Account account0 = Account.ofHistory(trades0);
		Account account1 = Account.ofHistory(trades1);
		double amount0 = account0.cash();
		double amount1 = account1.cash();

		return Read //
				.from2(Trade_.portfolio(trades0)) //
				.map((symbol, nShares) -> {
					Asset asset = cfg.queryCompany(symbol);
					float price = priceBySymbol.get(symbol);
					return asset + ": " + price + " * " + nShares + " = " + To.string(nShares * price);
				}) //
				.sort(Object_::compare) //
				.append("OWN = " + -amount0) //
				.append("P/L = " + amount1) //
				.append(account0.transactionSummary(cfg::transactionFee)) //
				.map(m -> "\n" + m) //
				.collect(As.joined());
	}

	private Streamlet<Trade> sellAll(Streamlet<Trade> trades, Map<String, Float> priceBySymbol) {
		return Streamlet.concat(trades, Trade_.sellAll(Trade.NA, trades, priceBySymbol::get));
	}

}
