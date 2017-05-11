package suite;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import suite.adt.Pair;
import suite.algo.Statistic;
import suite.math.MathUtil;
import suite.os.LogUtil;
import suite.os.SerializedStoreCache;
import suite.smtp.SmtpSslGmail;
import suite.streamlet.As;
import suite.streamlet.Read;
import suite.streamlet.Streamlet;
import suite.trade.Account;
import suite.trade.Asset;
import suite.trade.DatePeriod;
import suite.trade.Trade;
import suite.trade.TradeUtil;
import suite.trade.assetalloc.AssetAllocBackTest;
import suite.trade.assetalloc.AssetAllocBackTest.Simulate;
import suite.trade.assetalloc.AssetAllocator;
import suite.trade.assetalloc.MovingAvgMeanReversionAssetAllocator;
import suite.trade.data.Configuration;
import suite.trade.data.DataSource;
import suite.trade.data.QuoteDatabase;
import suite.trade.data.Summarize;
import suite.trade.singlealloc.BuySellStrategy;
import suite.trade.singlealloc.SingleAllocBackTest;
import suite.trade.singlealloc.Strategos;
import suite.util.FormatUtil;
import suite.util.FunUtil.Sink;
import suite.util.Serialize;
import suite.util.To;
import suite.util.Util;
import suite.util.Util.ExecutableProgram;

// mvn compile exec:java -Dexec.mainClass=suite.DailyMain
public class DailyMain extends ExecutableProgram {

	private Configuration cfg = new Configuration();
	private Statistic stat = new Statistic();

	public static void main(String[] args) {
		Util.run(DailyMain.class, args);
	}

	@Override
	protected boolean run(String[] args) {

		// fetch Yahoo historical data
		Map<String, DataSource> dataSourceBySymbol = cfg //
				.queryLeadingCompaniesByMarketCap(LocalDate.now()) //
				.map(asset -> asset.symbol) //
				.map2(cfg::dataSource) //
				.toMap();

		QuoteDatabase quoteDatabase = new QuoteDatabase();
		quoteDatabase.merge("o", dataSourceBySymbol);
		quoteDatabase.join();

		// perform systematic trading
		List<Pair<String, String>> outputs = Arrays.asList(bug(), mamr(), pmamr());
		StringBuilder sb = new StringBuilder();

		sb.append("\n" + new Summarize(cfg).summarize(To.sink(sb)));

		for (Pair<String, String> output : outputs) {
			sb.append("\n" + Constants.separator);
			sb.append("\nOUTPUT (" + output.t0 + "):" + output.t1 + "\n");
		}

		String result = sb.toString();
		LogUtil.info(result);

		SmtpSslGmail smtp = new SmtpSslGmail();
		smtp.send(null, getClass().getName(), result);
		return true;
	}

	// some orders caused by stupid bugs. need to sell those at suitable times.
	private Pair<String, String> bug() {
		String tag = "bug";
		StringBuilder sb = new StringBuilder();
		List<Trade> history = TradeUtil.fromHistory(r -> Util.stringEquals(r.strategy, tag));
		Account account = Account.fromPortfolio(history);

		Map<String, Float> faceValueBySymbol = Read.from(history) //
				.groupBy(record -> record.symbol, //
						rs -> (float) (Read.from(rs).collect(As.sumOfDoubles(r -> r.buySell * r.price))))
				.toMap();

		for (Entry<String, Integer> e : account.assets().entrySet()) {
			String symbol = e.getKey();
			int sell = e.getValue();
			double targetPrice = (1d + stat.riskFreeInterestRate) * faceValueBySymbol.get(symbol) / sell;
			sb.append("\nSIGNAL" + new Trade(-sell, symbol, (float) targetPrice));
		}

		return Pair.of(tag, sb.toString());
	}

	// moving average mean reversion
	private Pair<String, String> mamr() {
		String tag = "mamr";
		Streamlet<Asset> assets = cfg.getCompanies();
		BuySellStrategy strategy = new Strategos().movingAvgMeanReverting(64, 8, .15f);

		LogUtil.info("S0 pre-fetch quotes");

		cfg.quote(assets.map(asset -> asset.symbol).toSet());

		LogUtil.info("S1 perform back test");

		// identify stocks that are mean-reverting
		Map<String, Boolean> backTestBySymbol = SerializedStoreCache //
				.of(Serialize.mapOfString(Serialize.boolean_)) //
				.get(getClass().getSimpleName() + ".backTestBySymbol", () -> assets //
						.map2(stock -> stock.symbol, stock -> {
							try {
								DatePeriod period = DatePeriod.threeYears();
								DataSource ds0 = cfg.dataSource(stock.symbol, period);
								DataSource ds1 = ds0.range(period);

								ds1.validateTwoYears();
								SingleAllocBackTest backTest = SingleAllocBackTest.test(ds1, strategy);
								return MathUtil.isPositive(backTest.account.cash());
							} catch (Exception ex) {
								LogUtil.warn(ex.getMessage() + " for " + stock);
								return false;
							}
						}) //
						.toMap());

		LogUtil.info("S2 query lot sizes");

		DatePeriod period = DatePeriod.daysBefore(128);
		String sevenDaysAgo = FormatUtil.formatDate(LocalDate.now().plusDays(-7));
		List<String> messages = new ArrayList<>();

		LogUtil.info("S3 capture signals");

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
					String latestDate = FormatUtil.formatDate(LocalDate.now());
					float latestPrice = latest.values().iterator().next();

					DataSource ds1 = ds0.cons(latestDate, latestPrice);
					float[] prices = ds1.prices;

					int last = prices.length - 1;
					int signal = strategy.analyze(prices).get(last);
					String message = "\nSIGNAL" + new Trade(signal * asset.lotSize, symbol, latestPrice);

					if (signal != 0)
						messages.add(message);
				} catch (Exception ex) {
					LogUtil.warn(ex.getMessage() + " in " + prefix);
				}
			}
		}

		return Pair.of(tag, Read.from(messages).collect(As.joined()));
	}

	// portfolio-based moving average mean reversion
	private Pair<String, String> pmamr() {
		String tag = "pmamr";
		StringBuilder sb = new StringBuilder();
		Sink<String> log = To.sink(sb);
		Streamlet<Asset> assets = cfg.queryLeadingCompaniesByMarketCap(LocalDate.now()); // hkex.getCompanies()
		AssetAllocator assetAllocator = new MovingAvgMeanReversionAssetAllocator(cfg, log);
		Simulate sim = AssetAllocBackTest.of(cfg, assets, assetAllocator, log).simulate(300000f);

		Account account0 = Account.fromPortfolio(TradeUtil.fromHistory(r -> Util.stringEquals(r.strategy, tag)));
		Account account1 = sim.account;

		Set<String> symbols = To.set(account0.assets().keySet(), account1.assets().keySet());
		Map<String, Float> priceBySymbol = cfg.quote(symbols);
		List<Trade> trades = TradeUtil.diff(account0.assets(), account1.assets(), priceBySymbol);

		sb.append("\n" + sim.conclusion());
		sb.append(Read.from(trades).map(trade -> "\nSIGNAL" + trade).collect(As.joined()));

		return Pair.of(tag, sb.toString());
	}

}
