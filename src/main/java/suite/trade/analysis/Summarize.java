package suite.trade.analysis;

import static java.lang.Math.expm1;
import static java.lang.Math.log;

import java.time.LocalDate;
import java.util.Map;

import primal.MoreVerbs.Read;
import primal.Verbs.Build;
import primal.Verbs.Compare;
import primal.adt.Fixie;
import primal.adt.Fixie_.Fixie3;
import primal.fp.Funs.Fun;
import primal.fp.Funs.Iterate;
import primal.fp.Funs.Sink;
import primal.primitive.Dbl_Dbl;
import primal.primitive.adt.pair.LngFltPair;
import primal.streamlet.Streamlet;
import suite.trade.Account;
import suite.trade.Account.TransactionSummary;
import suite.trade.Time;
import suite.trade.Trade;
import suite.trade.Trade_;
import suite.trade.data.Broker.Hsbc;
import suite.trade.data.HkexUtil;
import suite.trade.data.TradeCfg;
import suite.trade.data.Yahoo;
import suite.ts.Quant;
import suite.util.FormatUtil;
import suite.util.To;

public class Summarize {

	private TradeCfg cfg;
	private Fun<String, LngFltPair[]> dividendFun = new Yahoo()::dividend;
	private Dbl_Dbl dividendFeeFun = new Hsbc()::dividendFee;

	public final Streamlet<Trade> trades;
	public final Map<String, Float> priceBySymbol;

	public static Summarize of(TradeCfg cfg) {
		return of(cfg, cfg.queryHistory());
	}

	public static Summarize of(TradeCfg cfg, Streamlet<Trade> trades) {
		var priceBySymbol = cfg.quote(trades.map(trade -> trade.symbol).toSet());
		return new Summarize(cfg, trades, priceBySymbol);
	}

	private Summarize(TradeCfg cfg, Streamlet<Trade> trades, Map<String, Float> priceBySymbol) {
		this.cfg = cfg;
		this.trades = trades;
		this.priceBySymbol = priceBySymbol;
	}

	public SummarizeByStrategy<Object> summarize() {
		return summarize(trade -> null);
	}

	public <K> SummarizeByStrategy<K> summarize(Fun<Trade, K> fun) {
		var summaryByKey = trades //
				.groupBy(fun, trades_ -> summarize_(trades_, priceBySymbol, s -> null)) //
				.filterKey(key -> key != null) //
				.collect();

		var nSharesByKeyBySymbol = summaryByKey //
				.concatMap((key, summary) -> summary.account //
						.portfolio() //
						.map((symbol, n) -> Fixie.of(symbol, key, n))) //
				.groupBy(Fixie3::get0, fixies0 -> fixies0 //
						.groupBy(Fixie3::get1, fixies1 -> fixies1 //
								.map(Fixie3::get2).uniqueResult())
						.toMap()) //
				.toMap();

		var acquiredPrices = trades.collect(Trade_::collectBrokeredTrades).collect(Trade_::collectAcquiredPrices);
		var now = Time.now();

		var overall = summarize_(trades, priceBySymbol, symbol -> {
			var isMarketOpen = false //
					|| HkexUtil.isMarketOpen(now) //
					|| HkexUtil.isMarketOpen(now.addHours(1));

			var ds = cfg.dataSource(symbol);
			var price0 = acquiredPrices.get(symbol); // acquisition price
			var price1 = ds.get(isMarketOpen ? -1 : -2).t1; // previous close
			var pricex = isMarketOpen ? priceBySymbol.get(symbol) : ds.get(-1).t1; // now

			var keys = Read //
					.from2(nSharesByKeyBySymbol.getOrDefault(symbol, Map.ofEntries())) //
					.keys() //
					.map(Object::toString) //
					.sort(Compare::string) //
					.toJoinedString("/");

			return percent(price1, pricex) //
					+ ", " + percent(price0, pricex) //
					+ (!keys.isEmpty() ? ", " + keys : "");
		});

		var text = Build.string(sb -> {
			Sink<String> log = sb::append;

			var outs = summaryByKey //
					.mapValue(Summarize_::out0) //
					.sortByKey(Compare::anyway) //
					.map((k, v) -> "\nFor strategy " + k + ":" + v);

			for (var out : outs)
				log.f(out);

			log.f(FormatUtil.tablize("\nOverall:\t" + Time.now().ymdHms() + overall.out1()));
		});

		// profit and loss
		var pnlByKey = sellAll(trades, priceBySymbol) //
				.groupBy(fun, t -> (double) Account.ofHistory(t).cash()) //
				.toMap();

		return new SummarizeByStrategy<>(text, overall.account, pnlByKey);
	}

	public class SummarizeByStrategy<K> {
		public final String log;
		public final Account overall;
		public final Map<K, Double> pnlByKey;

		private SummarizeByStrategy(String log, Account overall, Map<K, Double> pnlByKey) {
			this.log = log;
			this.overall = overall;
			this.pnlByKey = pnlByKey;
		}
	}

	private Summarize_ summarize_( //
			Streamlet<Trade> trades_, //
			Map<String, Float> priceBySymbol, //
			Iterate<String> infoFun) {
		var trades0 = trades_;
		var trades1 = sellAll(trades0, priceBySymbol);

		var details = Read //
				.from2(Trade_.portfolio(trades0)) //
				.map((symbol, nShares) -> {
					var instrument = cfg.queryCompany(symbol);
					var price = priceBySymbol.get(symbol);
					var info = infoFun.apply(symbol);
					return instrument //
							+ ": " + price + " * " + nShares //
							+ " = " + ((long) (nShares * price)) //
							+ (info != null ? " \t(" + info + ")" : "");
				}) //
				.sort(Compare::objects) //
				.collect();

		return new Summarize_(details, trades0, trades1);
	}

	private class Summarize_ {
		public final Streamlet<String> details;
		public final Streamlet<Trade> trades;
		public final Account account;
		public final double size, pnl, dividend;
		public final TransactionSummary transactionSummary;

		public Summarize_(Streamlet<String> details, Streamlet<Trade> trades0, Streamlet<Trade> trades1) {
			var accountTx = Account.ofHistory(trades0.collect(Trade_::collectBrokeredTrades));
			var account0 = Account.ofHistory(trades0);
			var account1 = Account.ofHistory(trades1);
			var amount0 = account0.cash();
			var amount1 = account1.cash();

			this.details = details;
			trades = trades0;
			account = account0;
			size = amount1 - amount0;
			pnl = amount1;
			dividend = Trade_.dividend(trades, dividendFun, dividendFeeFun);
			transactionSummary = accountTx.transactionSummary(cfg::transactionFee);
		}

		public String out0() {
			return details //
					.snoc("" //
							+ "size:" + To.string(size) //
							+ ", pnl:" + To.string(pnl) //
							+ ", div:" + To.string(dividend) //
							+ ", " + transactionSummary.out0()) //
					.toString();
		}

		public String out1() {
			var start = Time.of(trades.first().date).epochDay();
			var end = LocalDate.now().toEpochDay();
			var nYears = (end - start) / 365.25d;
			var nav0 = size - pnl;
			var nav1 = nav0 + pnl + dividend - transactionSummary.transactionFee;
			var rtn = nav1 / nav0;
			var cagr = expm1(log(rtn) / nYears);

			return details //
					.snoc("SIZ = " + To.string(size)) //
					.snoc("PNL = " + To.string(pnl)) //
					.snoc("DIV = " + To.string(dividend)) //
					.snoc(transactionSummary.out1()) //
					.snoc("CAGR = " + To.percent(cagr)) //
					.toString();
		}
	}

	private String percent(float price1, float pricex) {
		var pc = To.percent(Quant.return_(price1, pricex));
		return (pc.startsWith("-") ? "" : "+") + pc;
	}

	private Streamlet<Trade> sellAll(Streamlet<Trade> trades, Map<String, Float> priceBySymbol) {
		return Streamlet.concat(trades, Trade_.sellAll(Trade.NA, trades, priceBySymbol::get));
	}

}
