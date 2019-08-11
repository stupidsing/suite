package suite;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import primal.Verbs.Equals;
import primal.Verbs.Union;
import primal.adt.Pair;
import primal.fp.Funs.Sink;
import primal.os.Log_;
import primal.primitive.DblMoreVerbs.LiftDbl;
import primal.primitive.fp.AsDbl;
import primal.streamlet.Streamlet;
import suite.math.Math_;
import suite.node.util.Singleton;
import suite.os.SerializedStoreCache;
import suite.serialize.Serialize;
import suite.smtp.SmtpSslGmail;
import suite.streamlet.Read;
import suite.trade.Account;
import suite.trade.Instrument;
import suite.trade.Time;
import suite.trade.TimeRange;
import suite.trade.Trade;
import suite.trade.Trade_;
import suite.trade.analysis.Summarize;
import suite.trade.backalloc.BackAllocConfiguration;
import suite.trade.backalloc.BackAllocConfigurations;
import suite.trade.backalloc.BackAllocConfigurations.Bacs;
import suite.trade.backalloc.BackAllocTester;
import suite.trade.backalloc.BackAllocator;
import suite.trade.backalloc.strategy.BackAllocatorOld;
import suite.trade.data.HkexUtil;
import suite.trade.data.TradeCfg;
import suite.trade.data.TradeCfgImpl;
import suite.trade.singlealloc.SingleAllocBackTest;
import suite.trade.singlealloc.Strategos;
import suite.util.RunUtil;
import suite.util.To;

// mvn compile exec:java -Dexec.mainClass=suite.DailyMain
public class DailyMain {

	private Set<String> blackList = Set.of("0566.HK");

	private TradeCfg cfg = new TradeCfgImpl();
	private Serialize ser = Singleton.me.serialize;
	private StringBuilder sb = new StringBuilder();
	private Sink<String> log = To.sink(sb);
	private Time today = Time.now();

	private Bacs bacs = new BackAllocConfigurations(cfg).bacs();

	private class Result {
		private String strategy;
		private List<Trade> trades;

		private Result(String strategy, List<Trade> trades) {
			this.strategy = strategy;
			this.trades = trades;
		}
	}

	public static void main(String[] args) {
		RunUtil.run(new DailyMain()::run);
	}

	private boolean run() {
		Trade_.blackList = Union.of(Trade_.blackList, blackList);

		var sellPool = "sellpool";
		var ymd = HkexUtil.getCloseTimeBefore(Time.now()).ymd();
		var td = ymd + "#";

		// perform systematic trading
		var results = Read.each( //
				alloc(bacs.pair_bb, 66666f), //
				alloc("bug", bacs.bac_sell, 0f), //
				alloc(bacs.pair_donchian, 100000f), //
				alloc(bacs.pair_ema, 0f), //
				mamr(50000f), //
				alloc(bacs.pair_pmamr, 150000f), //
				alloc(bacs.pair_pmamr2, 366666f), //
				alloc(bacs.pair_pmmmr, 80000f), //
				alloc(bacs.pair_revco, 0f), //
				alloc(bacs.pair_tma, 0f), //
				alloc(sellPool, bacs.bac_sell, 0f));

		// unused strategies
		if (Boolean.FALSE) {
			alloc(bacs.pair_donchian, 100000f);
			pairs(0f, "0341.HK", "0052.HK");
			sellForEarn(sellPool);
		}

		var sbs = Summarize.of(cfg).summarize(trade -> trade.strategy);

		var strategyTrades = results //
				.concatMap2(result -> Read.from(result.trades).map2(trade -> result.strategy, trade -> trade)) //
				.filterValue(trade -> trade.buySell != 0) //
				.collect();

		var requestTrades = strategyTrades.filterKey(strategy -> !Equals.string(strategy, sellPool));
		var amounts = strategyTrades.values().collect(LiftDbl.of(Trade::amount));
		var buys_ = amounts.filter(amount -> 0d < amount).sum();
		var sells = amounts.filter(amount -> amount < 0d).sum();

		sb.append(sbs.log //
				+ "\n" + sbs.pnlByKey //
				+ "\nBUY REQUESTS" //
				+ requestTrades //
						.filterValue(trade -> 0 < trade.buySell) //
						.sortByValue(Trade::compare) //
						.map((strategy, t) -> "" //
								+ Trade.of(td, -t.buySell, t.symbol, t.price, sellPool).record() + "\n" //
								+ Trade.of(td, +t.buySell, t.symbol, t.price, strategy).record()) //
				+ "\n" //
				+ "\nSELL REQUESTS" //
				+ requestTrades //
						.filterValue(trade -> trade.buySell < 0) //
						.sortByValue(Trade::compare) //
						.map((strategy, t) -> "" //
								+ Trade.of(td, +t.buySell, t.symbol, t.price, strategy).record() + "\n" //
								+ Trade.of(td, -t.buySell, t.symbol, t.price, sellPool).record()) //
				+ "\n" //
				+ "\nTOTAL BUYS_ = " + To.string(buys_) //
				+ "\nTOTAL SELLS = " + To.string(sells) //
				+ "\n" //
				+ "\nSUGGESTIONS" //
				+ "\n- check your balance" //
				+ "\n- sell mamr and " + sellPool //
				+ "\n");

		var result = sb.toString().replace(".0\t", "\t");
		Log_.info(result);

		var smtp = new SmtpSslGmail();
		smtp.send(null, getClass().getName(), result);
		return true;
	}

	// moving average mean reversion
	private Result mamr(float factor) {
		var tag = "mamr";
		var nHoldDays = 8;
		var instruments = cfg.queryCompanies();
		var strategy = new Strategos().movingAvgMeanReverting(64, nHoldDays, .15f);

		// pre-fetch quotes
		cfg.quote(instruments.map(instrument -> instrument.symbol).toSet());

		// identify stocks that are mean-reverting
		var backTestBySymbol = SerializedStoreCache //
				.of(ser.mapOfString(ser.boolean_)) //
				.get(getClass().getSimpleName() + ".backTestBySymbol", () -> instruments //
						.map2(stock -> stock.symbol, stock -> {
							try {
								var period = TimeRange.threeYears();
								var ds = cfg.dataSource(stock.symbol, period).range(period).validate();
								var backTest = SingleAllocBackTest.test(ds, strategy);
								return Math_.isPositive(backTest.account.cash());
							} catch (Exception ex) {
								Log_.warn(ex + " for " + stock);
								return false;
							}
						}) //
						.toMap());

		var period = TimeRange.daysBefore(128);
		var trades = new ArrayList<Trade>();

		// capture signals
		for (var instrument : instruments) {
			var symbol = instrument.symbol;

			if (backTestBySymbol.get(symbol) == Boolean.TRUE)
				try {
					var ds = cfg.dataSource(symbol, period).validate();
					var prices = ds.prices;
					var last = prices.length - 1;
					var latestPrice = prices[last];

					var signal = strategy.analyze(prices).get(last);
					var nShares = signal * instrument.lotSize * Math.round(factor / nHoldDays / (instrument.lotSize * latestPrice));
					var trade = Trade.of(nShares, symbol, latestPrice);

					if (signal != 0)
						trades.add(trade);
				} catch (Exception ex) {
					Log_.warn(ex.getMessage() + " in " + instrument);
				}
		}

		return new Result(tag, trades);
	}

	private Result pairs(float fund, String symbol0, String symbol1) {
		return alloc("pairs/" + symbol0 + "/" + symbol1, pairs(symbol0, symbol1), fund);
	}

	public BackAllocConfiguration pairs(String symbol0, String symbol1) {
		var instruments = Read.each(symbol0, symbol1).map(cfg::queryCompany).collect();
		var backAllocator = BackAllocatorOld.me.pairs(cfg, symbol0, symbol1).unleverage();
		return new BackAllocConfiguration(time -> instruments, backAllocator);
	}

	// some orders caused by stupid bugs. need to sell those at suitable times.
	private Result sellForEarn(String tag) {
		var history = cfg.queryHistory().filter(r -> Equals.string(r.strategy, tag));
		var account = Account.ofPortfolio(history);

		var faceValueBySymbol = history //
				.groupBy(record -> record.symbol, rs -> (float) Read.from(rs).toDouble(AsDbl.sum(Trade::amount))) //
				.toMap();

		var trades = account //
				.portfolio() //
				.map((symbol, sell) -> {
					var targetPrice = (1d + 3 * Trade_.riskFreeInterestRate) * faceValueBySymbol.get(symbol) / sell;
					return Trade.of(-sell, symbol, (float) targetPrice);
				}) //
				.toList();

		return new Result(tag, trades);
	}

	private Result alloc(Pair<String, BackAllocConfiguration> pair, float fund) {
		return pair.map((tag, bac) -> alloc(tag, fund, bac.backAllocator, bac.instrumentsFun.apply(today)));
	}

	private Result alloc(String tag, BackAllocConfiguration pair, float fund) {
		return alloc(tag, fund, pair.backAllocator, pair.instrumentsFun.apply(today));
	}

	private Result alloc(String tag, float fund, BackAllocator backAllocator, Streamlet<Instrument> instruments) {
		var period = TimeRange.daysBefore(64);
		var sim = BackAllocTester.of(cfg, period, instruments, backAllocator, log).simulate(fund);
		var account0 = Account.ofPortfolio(cfg.queryHistory().filter(r -> Equals.string(r.strategy, tag)));
		var account1 = sim.account;
		var assets0 = account0.assets();
		var assets1 = account1.assets();

		var symbols = Union.of(assets0.keySet(), assets1.keySet());
		var priceBySymbol = cfg.quote(symbols);
		var trades = Trade_.diff(Trade.NA, assets0, assets1, priceBySymbol::get).toList();

		sb.append("\nstrategy = " + tag + ", " + sim.conclusion());

		return new Result(tag, trades);
	}

}
