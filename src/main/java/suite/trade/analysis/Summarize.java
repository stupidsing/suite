package suite.trade.analysis;

import java.util.Collections;
import java.util.Map;
import java.util.Map.Entry;

import suite.adt.pair.Fixie;
import suite.adt.pair.Fixie_.Fixie3;
import suite.math.stat.Quant;
import suite.primitive.Dbl_Dbl;
import suite.primitive.adt.pair.LngFltPair;
import suite.streamlet.As;
import suite.streamlet.Read;
import suite.streamlet.Streamlet;
import suite.streamlet.Streamlet2;
import suite.trade.Account;
import suite.trade.Account.TransactionSummary;
import suite.trade.Asset;
import suite.trade.Time;
import suite.trade.Trade;
import suite.trade.Trade_;
import suite.trade.data.Broker.Hsbc;
import suite.trade.data.Configuration;
import suite.trade.data.DataSource;
import suite.trade.data.HkexUtil;
import suite.trade.data.Yahoo;
import suite.util.FormatUtil;
import suite.util.FunUtil.Fun;
import suite.util.FunUtil.Iterate;
import suite.util.FunUtil.Sink;
import suite.util.Object_;
import suite.util.String_;
import suite.util.To;

public class Summarize {

	private Configuration cfg;
	private Fun<String, LngFltPair[]> dividendFun = new Yahoo()::dividend;
	private Dbl_Dbl dividendFeeFun = new Hsbc()::dividendFee;

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
		Streamlet2<K, Summarize_> summaryByKey = trades //
				.groupBy(fun, trades_ -> summarize_(trades_, priceBySymbol, s -> null)) //
				.filterKey(key -> key != null) //
				.collect(As::streamlet2);

		Map<String, Map<K, Integer>> nSharesByKeyBySymbol = summaryByKey //
				.concatMap((key, summary) -> summary.account //
						.portfolio() //
						.map((symbol, n) -> Fixie.of(symbol, key, n))) //
				.groupBy(Fixie3::get0, fixies0 -> fixies0 //
						.groupBy(Fixie3::get1, fixies1 -> fixies1 //
								.map(Fixie3::get2).uniqueResult())
						.toMap()) //
				.toMap();

		Map<String, Float> acquiredPrices = trades.collect(Trade_::collectBrokeredTrades).collect(Trade_::collectAcquiredPrices);
		Time now = Time.now();

		Summarize_ overall = summarize_(trades, priceBySymbol, symbol -> {
			boolean isMarketOpen = false //
					|| HkexUtil.isMarketOpen(now) //
					|| HkexUtil.isMarketOpen(now.addHours(1));

			DataSource ds = cfg.dataSource(symbol);
			float price0 = acquiredPrices.get(symbol); // acquisition price
			float price1 = ds.get(isMarketOpen ? -1 : -2).t1; // previous close
			float pricex = isMarketOpen ? priceBySymbol.get(symbol) : ds.get(-1).t1; // now

			String keys = Read //
					.from2(nSharesByKeyBySymbol.getOrDefault(symbol, Collections.emptyMap())) //
					.keys() //
					.map(Object::toString) //
					.sort(String_::compare) //
					.collect(As.joinedBy("/"));

			return percent(price1, pricex) //
					+ ", " + percent(price0, pricex) //
					+ (!keys.isEmpty() ? ", " + keys : "");
		});

		Map<K, String> outByKey = summaryByKey.mapValue(Summarize_::out0).toMap();
		StringBuilder sb = new StringBuilder();
		Sink<String> log = sb::append;

		for (Entry<K, String> e : outByKey.entrySet())
			log.sink("\nFor strategy " + e.getKey() + ":" + e.getValue());

		log.sink(FormatUtil.tablize("\nOverall:\t" + Time.now().ymdHms() + overall.out1()));

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

		Streamlet<String> details = Read //
				.from2(Trade_.portfolio(trades0)) //
				.map((symbol, nShares) -> {
					Asset asset = cfg.queryCompany(symbol);
					float price = priceBySymbol.get(symbol);
					String info = infoFun.apply(symbol);
					return asset //
							+ ": " + price + " * " + nShares //
							+ " = " + ((long) (nShares * price)) //
							+ (info != null ? " \t(" + info + ")" : "");
				}) //
				.sort(Object_::compare) //
				.collect(As::streamlet);

		return new Summarize_(details, trades0, trades1);
	}

	private class Summarize_ {
		public final Streamlet<String> details;
		public final Streamlet<Trade> trades;
		public final Account account;
		public final double size, pnl, dividend;
		public final TransactionSummary transactionSummary;

		public Summarize_(Streamlet<String> details, Streamlet<Trade> trades0, Streamlet<Trade> trades1) {
			Account accountTx = Account.ofHistory(trades0.collect(Trade_::collectBrokeredTrades));
			Account account0 = Account.ofHistory(trades0);
			Account account1 = Account.ofHistory(trades1);
			double amount0 = account0.cash();
			double amount1 = account1.cash();

			this.details = details;
			this.trades = trades0;
			this.account = account0;
			size = amount1 - amount0;
			pnl = amount1;
			dividend = Trade_.dividend(trades, dividendFun, dividendFeeFun);
			transactionSummary = accountTx.transactionSummary(cfg::transactionFee);
		}

		public String out0() {
			return details //
					.append("siz/pnl/div =" //
							+ " " + To.string(size) //
							+ "/" + To.string(pnl) //
							+ "/" + To.string(dividend)) //
					.append(transactionSummary.out0()) //
					.map(m -> "\n" + m) //
					.collect(As::joined);
		}

		public String out1() {
			return details //
					.append("SIZ = " + To.string(size)) //
					.append("PNL = " + To.string(pnl)) //
					.append("DIV = " + To.string(dividend)) //
					.append(transactionSummary.out1()) //
					.map(m -> "\n" + m) //
					.collect(As::joined);
		}
	}

	private String percent(float price1, float pricex) {
		String pc = String.format("%.1f", 100d * Quant.return_(price1, pricex)) + "%";
		return (pc.startsWith("-") ? "" : "+") + pc;
	}

	private Streamlet<Trade> sellAll(Streamlet<Trade> trades, Map<String, Float> priceBySymbol) {
		return Streamlet.concat(trades, Trade_.sellAll(Trade.NA, trades, priceBySymbol::get));
	}

}
