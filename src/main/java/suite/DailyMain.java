package suite;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import suite.adt.Pair;
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
import suite.trade.Portfolio;
import suite.trade.Trade;
import suite.trade.TradeUtil;
import suite.trade.assetalloc.MovingAvgMeanReversionAssetAllocator;
import suite.trade.data.DataSource;
import suite.trade.data.Hkex;
import suite.trade.data.HkexFactBook;
import suite.trade.data.QuoteDatabase;
import suite.trade.data.Yahoo;
import suite.trade.singlealloc.BackTest;
import suite.trade.singlealloc.BuySellStrategy;
import suite.trade.singlealloc.Strategos;
import suite.util.FormatUtil;
import suite.util.FunUtil.Sink;
import suite.util.Serialize;
import suite.util.To;
import suite.util.Util;
import suite.util.Util.ExecutableProgram;

// mvn compile exec:java -Dexec.mainClass=suite.DailyMain
public class DailyMain extends ExecutableProgram {

	private HkexFactBook hkexFactBook = new HkexFactBook();
	private Yahoo yahoo = new Yahoo();

	public static void main(String[] args) {
		Util.run(DailyMain.class, args);
	}

	@Override
	protected boolean run(String[] args) {

		// fetch Yahoo historical data
		Map<String, DataSource> dataSourceByStockCode = hkexFactBook //
				.queryLeadingCompaniesByMarketCap(LocalDate.now().getYear() - 1) //
				.map(asset -> asset.code) //
				.map2(stockCode -> yahoo.dataSource(stockCode)) //
				.toMap();

		new QuoteDatabase().merge("o", dataSourceByStockCode);

		// perform systematic trading
		List<Pair<String, String>> outputs = Arrays.asList(bug(), mamr(), pmamr());
		StringBuilder sb = new StringBuilder();

		for (Pair<String, String> output : outputs) {
			sb.append("--------------------------------------------------------------------------------\n");
			sb.append("OUTPUT (" + output.t0 + "):" + output.t1 + "\n\n");
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
		List<Trade> history = TradeUtil.fromHistory(r -> Util.stringEquals(r.strategy, "bug"));
		Account account = Account.fromHistory(history);

		Map<String, Float> faceValueByStockCodes = Read.from(history) //
				.groupBy(record -> record.stockCode, //
						rs -> (float) (Read.from(rs).collect(As.sumOfDoubles(r -> r.buySell * r.price))))
				.toMap();

		for (Entry<String, Integer> e : account.assets().entrySet()) {
			String stockCode = e.getKey();
			String targetPrice = To.string(-1.05d * faceValueByStockCodes.get(stockCode) / e.getValue());
			sb.append("\n" + stockCode + " has signal " + targetPrice + " * " + e.getValue());
		}

		return Pair.of(tag, sb.toString());
	}

	// moving average mean reversion
	private Pair<String, String> mamr() {
		String tag = "mamr";
		Hkex hkex = new Hkex();
		Yahoo yahoo = new Yahoo();
		Streamlet<Asset> assets = hkex.getCompanies();
		BuySellStrategy strategy = new Strategos().movingAvgMeanReverting(64, 8, .15f);

		LogUtil.info("S0 pre-fetch quotes");
		yahoo.quote(assets.map(asset -> asset.code).toSet());

		LogUtil.info("S1 perform back test");

		// identify stocks that are mean-reverting
		Map<String, Boolean> backTestByStockCode = SerializedStoreCache //
				.of(Serialize.mapOfString(Serialize.boolean_)) //
				.get(getClass().getSimpleName() + ".backTestByStockCode", () -> assets //
						.map2(stock -> stock.code, stock -> {
							try {
								DatePeriod period = DatePeriod.threeYears();
								DataSource ds0 = yahoo.dataSource(stock.code, period);
								DataSource ds1 = ds0.range(period);

								ds1.validateTwoYears();
								BackTest backTest = BackTest.test(ds1, strategy);
								return MathUtil.isPositive(backTest.account.cash());
							} catch (Exception ex) {
								LogUtil.warn(ex.getMessage() + " for " + stock);
								return false;
							}
						}) //
						.toMap());

		LogUtil.info("S2 query lot sizes");

		Map<String, Integer> lotSizeByStockCode = hkex.queryLotSizeByStockCode(assets);

		DatePeriod period = DatePeriod.daysBefore(128);
		String sevenDaysAgo = FormatUtil.formatDate(LocalDate.now().plusDays(-7));
		List<String> messages = new ArrayList<>();

		LogUtil.info("S3 capture signals");

		for (Asset asset : assets) {
			String stockCode = asset.code;

			if (backTestByStockCode.get(stockCode)) {
				String prefix = asset.toString();
				int lotSize = lotSizeByStockCode.get(stockCode);

				try {
					DataSource ds0 = yahoo.dataSource(stockCode, period);
					String datex = ds0.last().date;

					if (0 <= datex.compareTo(sevenDaysAgo))
						ds0.validate();
					else
						throw new RuntimeException("ancient data: " + datex);

					Map<String, Float> latest = yahoo.quote(Collections.singleton(stockCode));
					String latestDate = FormatUtil.formatDate(LocalDate.now());
					float latestPrice = latest.values().iterator().next();

					DataSource ds1 = ds0.cons(latestDate, latestPrice);
					float[] prices = ds1.prices;

					int last = prices.length - 1;
					int signal = strategy.analyze(prices).get(last);
					String message = "\n" + asset + " has signal " + prices[last] + " * " + signal * lotSize;

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
		Portfolio portfolio = new Portfolio(new MovingAvgMeanReversionAssetAllocator(log), log);
		Account account0 = Account.fromHistory(TradeUtil.fromHistory(r -> Util.stringEquals(r.strategy, tag)));
		Account account1 = portfolio.simulateLatest(1000000f).account;

		List<Pair<String, Integer>> diffs = TradeUtil.diff(account0.assets(), account1.assets());
		Map<String, Float> priceByStockCode = yahoo.quote(Read.from(diffs).map(pair -> pair.t0).toSet());

		for (Pair<String, Integer> pair : diffs) {
			String stockCode = pair.t0;
			Float price = priceByStockCode.get(stockCode);
			sb.append("\n" + stockCode + " has signal " + price + " * " + pair.t1);
		}

		return Pair.of(tag, sb.toString());
	}

}
