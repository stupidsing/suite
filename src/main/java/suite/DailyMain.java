package suite;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import suite.adt.pair.Pair;
import suite.math.MathUtil;
import suite.os.LogUtil;
import suite.os.SerializedStoreCache;
import suite.smtp.SmtpSslGmail;
import suite.streamlet.As;
import suite.streamlet.Read;
import suite.streamlet.Streamlet;
import suite.streamlet.Streamlet2;
import suite.trade.Account;
import suite.trade.Asset;
import suite.trade.DatePeriod;
import suite.trade.Trade;
import suite.trade.Trade_;
import suite.trade.analysis.Summarize;
import suite.trade.assetalloc.AssetAllocBackTest;
import suite.trade.assetalloc.AssetAllocBackTest.Simulate;
import suite.trade.assetalloc.AssetAllocator;
import suite.trade.assetalloc.AssetAllocator_;
import suite.trade.assetalloc.MovingAvgMeanReversionAssetAllocator0;
import suite.trade.assetalloc.ReverseCorrelateAssetAllocator;
import suite.trade.data.Configuration;
import suite.trade.data.ConfigurationImpl;
import suite.trade.data.DataSource;
import suite.trade.data.QuoteDatabase;
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
	private LocalDate today = LocalDate.now();
	private Streamlet<Asset> assets = cfg.queryLeadingCompaniesByMarketCap(today);

	public final Pair<Streamlet<Asset>, AssetAllocator> pair_bb = Pair.of(assets, AssetAllocator_.bollingerBands1());
	public final Pair<Streamlet<Asset>, AssetAllocator> pair_pmamr = Pair.of(assets, MovingAvgMeanReversionAssetAllocator0.of(log));
	public final Pair<Streamlet<Asset>, AssetAllocator> pair_pmmmr = Pair.of(assets, AssetAllocator_.movingMedianMeanReversion());
	public final Pair<Streamlet<Asset>, AssetAllocator> pair_revco = Pair.of(assets, ReverseCorrelateAssetAllocator.of());

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

		// fetch Yahoo historical data
		Streamlet2<String, DataSource> dataSourceBySymbol = cfg //
				.queryLeadingCompaniesByMarketCap(today) //
				.map(asset -> asset.symbol) //
				.map2(cfg::dataSource) //
				.collect(As::streamlet2);

		QuoteDatabase quoteDatabase = new QuoteDatabase();
		quoteDatabase.merge("o", dataSourceBySymbol);
		quoteDatabase.join();

		// perform systematic trading
		List<Result> results = Arrays.asList( //
				alloc("bb", 450000f, pair_bb), //
				bug(), //
				mamr(75000f), //
				pairs(0f, "0052.HK", "0341.HK"), //
				pairs(0f, "0341.HK", "0052.HK"), //
				pmamr(75000f), //
				pmmmr(125000f), //
				questaQuella(60000f, "0052.HK", "0341.HK"), //
				questaQuella(200000f, "0670.HK", "1055.HK"), //
				alloc("revco", 80000f, pair_revco));

		sb.append("\n" + Summarize.of(cfg).out(log) + "\n");

		Streamlet2<String, Trade> strategyTrades = Read.from(results) //
				.concatMap2(result -> Read.from(result.trades).map2(trade -> result.strategy, trade -> trade)) //
				.filterValue(trade -> trade.buySell != 0);

		sb.append(strategyTrades //
				.map((strategy, trade) -> "\n" + (0 <= trade.buySell ? "BUY^" : "SELL") //
						+ " SIGNAL(" + strategy + ")" + trade //
						+ " = " + To.string(trade.buySell * trade.price)) //
				.sortBy(line -> line) //
				.collect(As.joined()));

		Streamlet<Trade> trades = strategyTrades.values();

		sb.append("\nTOTAL BUYS = " + trades.collectAsFloat(As.sumOfFloats(trade -> Math.max(0, trade.buySell) * trade.price)));
		sb.append("\nTOTAL SELLS = " + trades.collectAsFloat(As.sumOfFloats(trade -> Math.max(0, -trade.buySell) * trade.price)));

		sb.append("\nSUGGESTIONS");
		sb.append("\n- check your balance");
		sb.append("\n- get away with the small orders");
		sb.append("\n- for mamr, check actual execution using SingleAllocBackTestTest.testBackTestHkexDetails()");
		sb.append("\n");

		String result = sb.toString();
		LogUtil.info(result);

		SmtpSslGmail smtp = new SmtpSslGmail();
		smtp.send(null, getClass().getName(), result);
		return true;
	}

	// some orders caused by stupid bugs. need to sell those at suitable times.
	private Result bug() {
		String tag = "bug";
		Streamlet<Trade> history = cfg.queryHistory().filter(r -> String_.equals(r.strategy, tag));
		Account account = Account.fromPortfolio(history);

		Map<String, Float> faceValueBySymbol = history //
				.groupBy(record -> record.symbol, //
						rs -> (float) (Read.from(rs).collectAsDouble(As.sumOfDoubles(r -> r.buySell * r.price))))
				.toMap();

		List<Trade> trades = Read.from2(account.assets()) //
				.map((symbol, sell) -> {
					double targetPrice = (1d + 3 * Trade_.riskFreeInterestRate) * faceValueBySymbol.get(symbol) / sell;
					return Trade.of(-sell, symbol, (float) targetPrice);
				}) //
				.toList();

		return new Result(tag, trades);
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
								DatePeriod period = DatePeriod.threeYears();
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

		DatePeriod period = DatePeriod.daysBefore(128);
		String sevenDaysAgo = To.string(today.plusDays(-7));
		List<Trade> trades = new ArrayList<>();

		// capture signals
		for (Asset asset : assets) {
			String symbol = asset.symbol;

			if (backTestBySymbol.get(symbol)) {
				String prefix = asset.toString();

				try {
					DataSource ds0 = cfg.dataSource(symbol, period);
					String datex = ds0.last().date;

					if (0 <= datex.compareTo(sevenDaysAgo))
						ds0.validate();
					else
						throw new RuntimeException("ancient data: " + datex);

					Map<String, Float> latest = cfg.quote(Collections.singleton(symbol));
					String latestDate = To.string(today);
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

	// portfolio-based moving average mean reversion
	private Result pmamr(float fund) {
		return alloc("pmamr", fund, pair_pmamr);
	}

	// portfolio-based moving median mean reversion
	private Result pmmmr(float fund) {
		return alloc("pmmmr", fund, pair_pmmmr);
	}

	private Result questaQuella(float fund, String symbol0, String symbol1) {
		return alloc("qq/" + symbol0 + "/" + symbol1, fund, questaQuella(symbol0, symbol1));
	}

	public Pair<Streamlet<Asset>, AssetAllocator> pairs(String symbol0, String symbol1) {
		Streamlet<Asset> assets = Read.each(symbol0, symbol1).map(cfg::queryCompany).collect(As::streamlet);
		AssetAllocator assetAllocator = AssetAllocator_.byPairs(cfg, symbol0, symbol1);
		return Pair.of(assets, assetAllocator);
	}

	public Pair<Streamlet<Asset>, AssetAllocator> questaQuella(String symbol0, String symbol1) {
		Streamlet<Asset> assets = Read.each(symbol0, symbol1).map(cfg::queryCompany).collect(As::streamlet);
		AssetAllocator assetAllocator = AssetAllocator_.questoQuella(symbol0, symbol1);
		return Pair.of(assets, assetAllocator);
	}

	private Result alloc(String tag, float fund, Pair<Streamlet<Asset>, AssetAllocator> pair) {
		return alloc(tag, fund, pair.t1, pair.t0);
	}

	private Result alloc(String tag, float fund, AssetAllocator assetAllocator, Streamlet<Asset> assets) {
		Simulate sim = AssetAllocBackTest.ofNow(cfg, assets, assetAllocator, log).simulate(fund);

		Account account0 = Account.fromPortfolio(cfg.queryHistory().filter(r -> String_.equals(r.strategy, tag)));
		Account account1 = sim.account;
		Map<String, Integer> assets0 = account0.assets();
		Map<String, Integer> assets1 = account1.assets();

		Set<String> symbols = Set_.union(assets0.keySet(), assets1.keySet());
		Map<String, Float> priceBySymbol = cfg.quote(symbols);
		List<Trade> trades = Trade_.diff(assets0, assets1, priceBySymbol);

		sb.append("\n" + sim.conclusion());

		return new Result(tag, trades);
	}

}
