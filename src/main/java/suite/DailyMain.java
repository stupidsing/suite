package suite;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
import suite.trade.Trade_;
import suite.trade.assetalloc.AssetAllocBackTest;
import suite.trade.assetalloc.AssetAllocBackTest.Simulate;
import suite.trade.assetalloc.AssetAllocator;
import suite.trade.assetalloc.AssetAllocator_;
import suite.trade.assetalloc.MovingAvgMeanReversionAssetAllocator0;
import suite.trade.assetalloc.MovingMedianMeanReversionAssetAllocator;
import suite.trade.assetalloc.ReverseCorrelateAssetAllocator;
import suite.trade.data.Configuration;
import suite.trade.data.ConfigurationImpl;
import suite.trade.data.DataSource;
import suite.trade.data.QuoteDatabase;
import suite.trade.data.Summarize;
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
		Map<String, DataSource> dataSourceBySymbol = cfg //
				.queryLeadingCompaniesByMarketCap(LocalDate.now()) //
				.map(asset -> asset.symbol) //
				.map2(cfg::dataSource) //
				.toMap();

		QuoteDatabase quoteDatabase = new QuoteDatabase();
		quoteDatabase.merge("o", dataSourceBySymbol);
		quoteDatabase.join();

		// perform systematic trading
		List<Result> results = Arrays.asList(bug(), mamr(), pmamr(), pmmmr(), revco());

		sb.append("\n" + Summarize.of(cfg).out(log) + "\n");

		for (Result result : results)
			sb.append(Read.from(result.trades) //
					.filter(trade -> trade.buySell != 0) //
					.map(trade -> "\nSIGNAL(" + result.strategy + ")" + trade) //
					.collect(As.joined()));

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
	private Result mamr() {
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
		String sevenDaysAgo = To.string(LocalDate.now().plusDays(-7));
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
					String latestDate = To.string(LocalDate.now());
					float latestPrice = latest.values().iterator().next();

					DataSource ds1 = ds0.cons(latestDate, latestPrice);
					float[] prices = ds1.prices;

					int last = prices.length - 1;
					int signal = strategy.analyze(prices).get(last);
					int nShares = signal * asset.lotSize * Math.round(300000f / nHoldDays / (asset.lotSize * latestPrice));
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

	// portfolio-based moving average mean reversion
	private Result pmamr() {
		return alloc("pmamr", MovingAvgMeanReversionAssetAllocator0.of(cfg, log));
	}

	// portfolio-based moving median mean reversion
	private Result pmmmr() {
		return alloc("pmmmr", MovingMedianMeanReversionAssetAllocator.of());
	}

	// portfolio-based moving average mean reversion
	private Result revco() {
		return alloc("revco", AssetAllocator_.unleverage(ReverseCorrelateAssetAllocator.of()));
	}

	private Result alloc(String tag, AssetAllocator assetAllocator) {
		Streamlet<Asset> assets = cfg.queryLeadingCompaniesByMarketCap(LocalDate.now()); // hkex.getCompanies()
		Simulate sim = AssetAllocBackTest.ofNow(cfg, assets, assetAllocator, log).simulate(300000f);

		Account account0 = Account.fromPortfolio(cfg.queryHistory().filter(r -> String_.equals(r.strategy, tag)));
		Account account1 = sim.account;

		Set<String> symbols = Set_.union(account0.assets().keySet(), account1.assets().keySet());
		Map<String, Float> priceBySymbol = cfg.quote(symbols);
		List<Trade> trades = Trade_.diff(account0.assets(), account1.assets(), priceBySymbol);

		sb.append("\n" + sim.conclusion());

		return new Result(tag, trades);
	}

}
