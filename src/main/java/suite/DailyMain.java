package suite;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import suite.math.MathUtil;
import suite.os.LogUtil;
import suite.os.SerializedStoreCache;
import suite.primitive.DblPrimitives.Obj_Dbl;
import suite.smtp.SmtpSslGmail;
import suite.streamlet.As;
import suite.streamlet.Read;
import suite.streamlet.Streamlet;
import suite.streamlet.Streamlet2;
import suite.trade.Account;
import suite.trade.Asset;
import suite.trade.Time;
import suite.trade.TimeRange;
import suite.trade.Trade;
import suite.trade.Trade_;
import suite.trade.analysis.Summarize;
import suite.trade.analysis.Summarize.SummarizeByStrategy;
import suite.trade.backalloc.BackAllocConfiguration;
import suite.trade.backalloc.BackAllocConfigurations;
import suite.trade.backalloc.BackAllocConfigurations.Bacs;
import suite.trade.backalloc.BackAllocTester;
import suite.trade.backalloc.BackAllocTester.Simulate;
import suite.trade.backalloc.BackAllocator;
import suite.trade.backalloc.strategy.BackAllocatorOld;
import suite.trade.data.Configuration;
import suite.trade.data.ConfigurationImpl;
import suite.trade.data.DataSource;
import suite.trade.singlealloc.BuySellStrategy;
import suite.trade.singlealloc.SingleAllocBackTest;
import suite.trade.singlealloc.Strategos;
import suite.util.FunUtil.Sink;
import suite.util.Serialize;
import suite.util.Set_;
import suite.util.String_;
import suite.util.To;
import suite.util.Util;
import suite.util.Util.ExecutableProgram;

// mvn compile exec:java -Dexec.mainClass=suite.DailyMain
public class DailyMain extends ExecutableProgram {

	private Set<String> blackList = To.set("N/A", "1880.HK", "2973.HK"); // "0566.HK"

	private Configuration cfg = new ConfigurationImpl();
	private StringBuilder sb = new StringBuilder();
	private Sink<String> log = To.sink(sb);
	private Time today = Time.now();

	private Bacs bacs = new BackAllocConfigurations(cfg, log).bacs();

	private class Result {
		private String strategy;
		private List<Trade> trades;

		private Result(String strategy, List<Trade> trades) {
			this.strategy = strategy;
			this.trades = trades;
		}
	}

	public static void main(String[] args) {
		Util.run(DailyMain.class, args);
	}

	@Override
	protected boolean run(String[] args) {
		Trade_.blackList = Set_.union(Trade_.blackList, blackList);

		String sellPool = "sellpool";
		String ymd = Time.now().ymd();

		// perform systematic trading
		List<Result> results = Arrays.asList( //
				alloc("bb", 200000f, bacs.bac_bbHold), //
				alloc("bug", 0f, bacs.bac_sell), //
				alloc("donchian", 100000f, bacs.bac_donHold), //
				alloc("ema", 200000f, bacs.bac_ema), //
				mamr(100000f), //
				alloc("pmamr", 100000f, bacs.bac_pmamr), //
				alloc("pmmmr", 80000f, bacs.bac_pmmmr), //
				alloc("revco", 0f, bacs.bac_revco), //
				alloc("tma", 0f, bacs.bac_tma), //
				alloc(sellPool, 0f, bacs.bac_sell));

		// unused strategies
		if (Boolean.FALSE) {
			alloc("donchian", 100000f, bacs.bac_donHold);
			pairs(0f, "0341.HK", "0052.HK");
			sellForEarn(sellPool);
		}

		SummarizeByStrategy<Object> sbs = Summarize.of(cfg).summarize();
		sb.append(sbs.log);
		sb.append("\n" + sbs.pnlByKey + "\n");

		Streamlet2<String, Trade> strategyTrades = Read //
				.from(results) //
				.concatMap2(result -> Read.from(result.trades).map2(trade -> result.strategy, trade -> trade)) //
				.filterValue(trade -> trade.buySell != 0) //
				.collect(As::streamlet2);

		sb.append(strategyTrades //
				.map((strategy, trade) -> "\n" //
						+ (0 <= trade.buySell ? "BUY^" : "SELL") //
						+ " SIGNAL(" + strategy + ")" + trade //
						+ " = " + To.string(trade.buySell * trade.price)) //
				.sortBy(line -> line) //
				.collect(As::joined));

		sb.append("\n\nBUY REQUESTS");
		sb.append(strategyTrades //
				.filterKey(strategy -> !To.set(sellPool).contains(strategy)) //
				.filterValue(trade -> 0 < trade.buySell) //
				.map((strategy, t) -> "" //
						+ "\n" + Trade.of(ymd, -t.buySell, t.symbol, t.price, sellPool).record() //
						+ "\n" + Trade.of(ymd, t.buySell, t.symbol, t.price, strategy).record()) //
				.collect(As::joined));

		sb.append("\n\nSELL REQUESTS");
		sb.append(strategyTrades //
				.filterKey(strategy -> !To.set(sellPool).contains(strategy)) //
				.filterValue(trade -> trade.buySell < 0) //
				.map((strategy, t) -> "" //
						+ "\n" + Trade.of(ymd, t.buySell, t.symbol, t.price, strategy).record() //
						+ "\n" + Trade.of(ymd, -t.buySell, t.symbol, t.price, sellPool).record()) //
				.collect(As::joined));

		Streamlet<Trade> trades = strategyTrades.values();
		double buys_ = trades.collectAsDouble(Obj_Dbl.sum(trade -> Math.max(0, trade.buySell) * trade.price));
		double sells = trades.collectAsDouble(Obj_Dbl.sum(trade -> Math.max(0, -trade.buySell) * trade.price));

		sb.append("\n");
		sb.append("\nTOTAL BUYS_ = " + buys_);
		sb.append("\nTOTAL SELLS = " + sells);

		sb.append("\n");
		sb.append("\nSUGGESTIONS");
		sb.append("\n- check your balance");
		sb.append("\n- sort the orders and get away the small ones");
		sb.append("\n- get away the stocks after ex-date");
		sb.append("\n- sell mamr and " + sellPool);
		sb.append("\n- for mamr, check actual execution using SingleAllocBackTestTest.testBackTestHkexDetails()");

		sb.append("\n");

		String result = sb.toString();
		LogUtil.info(result);

		SmtpSslGmail smtp = new SmtpSslGmail();
		smtp.send(null, getClass().getName(), result);
		return true;
	}

	// moving average mean reversion
	private Result mamr(float factor) {
		String tag = "mamr";
		int nHoldDays = 8;
		Streamlet<Asset> assets = cfg.queryCompanies();
		BuySellStrategy strategy = new Strategos().movingAvgMeanReverting(64, nHoldDays, .15f);

		// pre-fetch quotes
		cfg.quote(assets.map(asset -> asset.symbol).toSet());

		// identify stocks that are mean-reverting
		Map<String, Boolean> backTestBySymbol = SerializedStoreCache //
				.of(Serialize.mapOfString(Serialize.boolean_)) //
				.get(getClass().getSimpleName() + ".backTestBySymbol", () -> assets //
						.map2(stock -> stock.symbol, stock -> {
							try {
								TimeRange period = TimeRange.threeYears();
								DataSource ds0 = cfg.dataSource(stock.symbol, period);
								DataSource ds1 = ds0.range(period);

								ds1.validate();
								SingleAllocBackTest backTest = SingleAllocBackTest.test(ds1, strategy);
								return MathUtil.isPositive(backTest.account.cash());
							} catch (Exception ex) {
								LogUtil.warn(ex + " for " + stock);
								return false;
							}
						}) //
						.toMap());

		TimeRange period = TimeRange.daysBefore(128);
		Time sevenDaysAgo = today.addDays(-7);
		List<Trade> trades = new ArrayList<>();

		// capture signals
		for (Asset asset : assets) {
			String symbol = asset.symbol;

			if (backTestBySymbol.get(symbol))
				try {
					DataSource ds0 = cfg.dataSource(symbol, period);
					Time timex = Time.ofEpochSec(ds0.last().t0);

					if (0 <= Time.compare(timex, sevenDaysAgo))
						ds0.validate();
					else
						throw new RuntimeException("ancient data: " + timex);

					Map<String, Float> latest = cfg.quote(Collections.singleton(symbol));
					long latestDate = today.startOfDay().epochSec();
					float latestPrice = latest.values().iterator().next();

					DataSource ds1 = ds0.cons(latestDate, latestPrice);
					float[] prices = ds1.prices;
					int last = prices.length - 1;

					int signal = strategy.analyze(prices).get(last);
					int nShares = signal * asset.lotSize * Math.round(factor / nHoldDays / (asset.lotSize * latestPrice));
					Trade trade = Trade.of(nShares, symbol, latestPrice);

					if (signal != 0)
						trades.add(trade);
				} catch (Exception ex) {
					LogUtil.warn(ex.getMessage() + " in " + asset);
				}
		}

		return new Result(tag, trades);
	}

	private Result pairs(float fund, String symbol0, String symbol1) {
		return alloc("pairs/" + symbol0 + "/" + symbol1, fund, pairs(symbol0, symbol1));
	}

	public BackAllocConfiguration pairs(String symbol0, String symbol1) {
		Streamlet<Asset> assets = Read.each(symbol0, symbol1).map(cfg::queryCompany).collect(As::streamlet);
		BackAllocator backAllocator = BackAllocatorOld.me.pairs(cfg, symbol0, symbol1).unleverage();
		return new BackAllocConfiguration(time -> assets, backAllocator);
	}

	// some orders caused by stupid bugs. need to sell those at suitable times.
	private Result sellForEarn(String tag) {
		Streamlet<Trade> history = cfg.queryHistory().filter(r -> String_.equals(r.strategy, tag));
		Account account = Account.ofPortfolio(history);

		Map<String, Float> faceValueBySymbol = history //
				.groupBy(record -> record.symbol, //
						rs -> (float) (Read.from(rs).collectAsDouble(Obj_Dbl.sum(r -> r.buySell * r.price))))
				.toMap();

		List<Trade> trades = account //
				.portfolio() //
				.map((symbol, sell) -> {
					double targetPrice = (1d + 3 * Trade_.riskFreeInterestRate) * faceValueBySymbol.get(symbol) / sell;
					return Trade.of(-sell, symbol, (float) targetPrice);
				}) //
				.toList();

		return new Result(tag, trades);
	}

	private Result alloc(String tag, float fund, BackAllocConfiguration pair) {
		return alloc(tag, fund, pair.backAllocator, pair.assetsFun.apply(today));
	}

	private Result alloc(String tag, float fund, BackAllocator backAllocator, Streamlet<Asset> assets) {
		TimeRange period = TimeRange.daysBefore(64);
		Simulate sim = BackAllocTester.of(cfg, period, assets, backAllocator, log).simulate(fund);
		Account account0 = Account.ofPortfolio(cfg.queryHistory().filter(r -> String_.equals(r.strategy, tag)));
		Account account1 = sim.account;
		Map<String, Integer> assets0 = account0.assets();
		Map<String, Integer> assets1 = account1.assets();

		Set<String> symbols = Set_.union(assets0.keySet(), assets1.keySet());
		Map<String, Float> priceBySymbol = cfg.quote(symbols);
		List<Trade> trades = Trade_.diff(Trade.NA, assets0, assets1, priceBySymbol::get).toList();

		sb.append("\nstrategy = " + tag + ", " + sim.conclusion());

		return new Result(tag, trades);
	}

}
