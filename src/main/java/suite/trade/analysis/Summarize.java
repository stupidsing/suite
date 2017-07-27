package suite.trade.analysis;

import java.util.Map;
import java.util.Map.Entry;

import suite.math.stat.Quant;
import suite.streamlet.As;
import suite.streamlet.Read;
import suite.streamlet.Streamlet;
import suite.trade.Account;
import suite.trade.Asset;
import suite.trade.Trade;
import suite.trade.Trade_;
import suite.trade.data.Configuration;
import suite.trade.data.DataSource;
import suite.util.FunUtil.Fun;
import suite.util.FunUtil.Iterate;
import suite.util.FunUtil.Sink;
import suite.util.Object_;

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
				.mapValue(trades_ -> summarize_(Read.from(trades_), priceBySymbol, s -> null).out) //
				.toMap();

		for (Entry<K, String> e : summaryByKey.entrySet()) {
			K key = e.getKey();
			String summary = e.getValue();
			log.sink("\nFor strategy " + key + ":" + summary);
		}

		Summarize_ overall = summarize_(trades, priceBySymbol, symbol -> {
			DataSource ds = cfg.dataSource(symbol);
			boolean isMarketOpen = true; // HkexUtil.isMarketOpen(Time.now());
			float price0 = ds.get(isMarketOpen ? -1 : -2).t1;
			float pricex = isMarketOpen ? priceBySymbol.get(symbol) : ds.get(-1).t1;
			String percent = String.format("%.1f", 100d * Quant.return_(price0, pricex)) + "%";
			return (percent.startsWith("-") ? "" : "+") + percent;
		});

		log.sink("\nOverall:" + overall.out);

		// profit and loss
		Map<K, Double> pnlByKey = sellAll(trades, priceBySymbol) //
				.groupBy(fun, t -> (double) Account.ofHistory(t).cash()) //
				.toMap();

		return new SummarizeByStrategy<>(sb.toString(), overall.account, pnlByKey);
	}

	public class SummarizeByStrategy<K> {
		public final String log;
		public final Account overall;
		public final Map<K, Double> pnlByKey;

		private SummarizeByStrategy(String log, Account overall, Map<K, Double> pnlBySymbol) {
			this.log = log;
			this.overall = overall;
			this.pnlByKey = pnlBySymbol;
		}
	}

	private Summarize_ summarize_( //
			Streamlet<Trade> trades_, //
			Map<String, Float> priceBySymbol, //
			Iterate<String> infoFun) {
		Streamlet<Trade> trades0 = trades_;
		Streamlet<Trade> trades1 = sellAll(trades0, priceBySymbol);
		Account account0 = Account.ofHistory(trades0);
		Account account1 = Account.ofHistory(trades1);
		double amount0 = account0.cash();
		double amount1 = account1.cash();

		String out = Read //
				.from2(Trade_.portfolio(trades0)) //
				.map((symbol, nShares) -> {
					Asset asset = cfg.queryCompany(symbol);
					float price = priceBySymbol.get(symbol);
					String info = infoFun.apply(symbol);
					return asset //
							+ ": " + price + " * " + nShares //
							+ " = " + ((long) (nShares * price)) //
							+ (info != null ? " (" + info + ")" : "");
				}) //
				.sort(Object_::compare) //
				.append("OWN = " + -amount0) //
				.append("P/L = " + amount1) //
				.append(account0.transactionSummary(cfg::transactionFee)) //
				.map(m -> "\n" + m) //
				.collect(As.joined());

		return new Summarize_(account0, out);
	}

	private class Summarize_ {
		public final Account account;
		public final String out;

		public Summarize_(Account account, String out) {
			this.account = account;
			this.out = out;
		}
	}

	private Streamlet<Trade> sellAll(Streamlet<Trade> trades, Map<String, Float> priceBySymbol) {
		return Streamlet.concat(trades, Trade_.sellAll(Trade.NA, trades, priceBySymbol::get));
	}

}
