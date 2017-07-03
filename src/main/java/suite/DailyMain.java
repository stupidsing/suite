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
import suite.primitive.FltPrimitives.Obj_Flt;
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
import suite.trade.backalloc.BackAllocConfiguration;
import suite.trade.backalloc.BackAllocConfigurations;
import suite.trade.backalloc.BackAllocTester;
import suite.trade.backalloc.BackAllocTester.Simulate;
import suite.trade.backalloc.BackAllocator;
import suite.trade.backalloc.BackAllocator_;
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

	private Configuration cfg = new ConfigurationImpl();
	private StringBuilder sb = new StringBuilder();
	private Sink<String> log = To.sink(sb);
	private Time today = Time.now();

	private BackAllocConfigurations bacs = new BackAllocConfigurations(cfg, log);

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
		String ymd = Time.now().ymd();

		// perform systematic trading
		List<Result> results = Arrays.asList( //
				alloc("bb", 100000f, bacs.bac_bb), //
				alloc("bug", 0f, bacs.bac_sell), //
				alloc("ema", 100000f, bacs.bac_ema), //
				alloc("facoil", 100000f, bacs.bac_facoil), //
				mamr(100000f), //
				alloc("pmamr", 100000f, bacs.bac_pmamr), //
				alloc("pmmmr", 120000f, bacs.bac_pmmmr), //
				alloc("revco", 80000f, bacs.bac_revco), //
				alloc("tma", 100000f, bacs.bac_tma), //
				alloc("sellpool", 0f, bacs.bac_sell));

		// unused strategies
		if (Boolean.FALSE) {
			alloc("donchian", 100000f, bacs.bac_donchian);
			pairs(0f, "0341.HK", "0052.HK");
			questoaQuella(200000f, "0670.HK", "1055.HK");
			sellForEarn("sellpool");
		}

		sb.append("\n" + Summarize.of(cfg).out(log) + "\n");

		Streamlet2<String, Trade> strategyTrades = Read.from(results) //
				.concatMap2(result -> Read.from(result.trades).map2(trade -> result.strategy, trade -> trade)) //
				.filterValue(trade -> trade.buySell != 0) //
				.collect(As::streamlet2);

		sb.append(strategyTrades //
				.map((strategy, trade) -> "\n" + (0 <= trade.buySell ? "BUY^" : "SELL") //
						+ " SIGNAL(" + strategy + ")" + trade //
						+ " = " + To.string(trade.buySell * trade.price)) //
				.sortBy(line -> line) //
				.collect(As.joined()));

		sb.append(strategyTrades //
				.filterKey(strategy -> !To.set("sellpool").contains(strategy)) //
				.filterValue(trade -> trade.buySell < 0) //
				.map((strategy, t) -> "" //
						+ "\n" + ymd + "\t" + t.buySell + "\t" + t.symbol + "\t" + t.price + "\t" + strategy //
						+ "\n" + ymd + "\t" + (-t.buySell) + "\t" + t.symbol + "\t" + t.price + "\tsellpool") //
				.collect(As.joined()));

		Streamlet<Trade> trades = strategyTrades.values();

		sb.append("\n");
		sb.append("\nTOTAL BUYS = " + trades.collectAsFloat(Obj_Flt.sum(trade -> Math.max(0, trade.buySell) * trade.price)));
		sb.append("\nTOTAL SELLS = " + trades.collectAsFloat(Obj_Flt.sum(trade -> Math.max(0, -trade.buySell) * trade.price)));

		sb.append("\n");
		sb.append("\nSUGGESTIONS");
		sb.append("\n- check your balance");
		sb.append("\n- get away with the small orders");
		sb.append("\n- get away with stocks after ex-date");
		sb.append("\n- sell mamr and sellpool, maximum hold 2 weeks");
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

			if (backTestBySymbol.get(symbol)) {
				String prefix = asset.toString();

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
					LogUtil.warn(ex.getMessage() + " in " + prefix);
				}
			}
		}

		return new Result(tag, trades);
	}

	private Result pairs(float fund, String symbol0, String symbol1) {
		return alloc("pairs/" + symbol0 + "/" + symbol1, fund, pairs(symbol0, symbol1));
	}

	private Result questoaQuella(float fund, String symbol0, String symbol1) {
		return alloc("qq/" + symbol0 + "/" + symbol1, fund, bacs.questoaQuella(symbol0, symbol1));
	}

	public BackAllocConfiguration pairs(String symbol0, String symbol1) {
		Streamlet<Asset> assets = Read.each(symbol0, symbol1).map(cfg::queryCompany).collect(As::streamlet);
		BackAllocator backAllocator = BackAllocator_.pairs(cfg, symbol0, symbol1).unleverage();
		return new BackAllocConfiguration(time -> assets, backAllocator);
	}

	// some orders caused by stupid bugs. need to sell those at suitable times.
	private Result sellForEarn(String tag) {
		Streamlet<Trade> history = cfg.queryHistory().filter(r -> String_.equals(r.strategy, tag));
		Account account = Account.fromPortfolio(history);

		Map<String, Float> faceValueBySymbol = history //
				.groupBy(record -> record.symbol, //
						rs -> (float) (Read.from(rs).collectAsDouble(Obj_Dbl.sum(r -> r.buySell * r.price))))
				.toMap();

		List<Trade> trades = Read.from2(account.assets()) //
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
		Simulate sim = BackAllocTester.ofNow(cfg, assets, backAllocator, log).simulate(fund);

		Account account0 = Account.fromPortfolio(cfg.queryHistory().filter(r -> String_.equals(r.strategy, tag)));
		Account account1 = sim.account;
		Map<String, Integer> assets0 = account0.assets();
		Map<String, Integer> assets1 = account1.assets();

		Set<String> symbols = Set_.union(assets0.keySet(), assets1.keySet());
		Map<String, Float> priceBySymbol = cfg.quote(symbols);
		List<Trade> trades = Trade_.diff(assets0, assets1, priceBySymbol);

		sb.append("\nstrategy = " + tag + ", " + sim.conclusion());

		return new Result(tag, trades);
	}

}
