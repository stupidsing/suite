package suite.trade;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import suite.adt.Pair;
import suite.algo.Statistic;
import suite.math.TimeSeries;
import suite.os.LogUtil;
import suite.streamlet.As;
import suite.streamlet.Read;
import suite.streamlet.Streamlet;
import suite.util.FormatUtil;
import suite.util.FunUtil.Fun;
import suite.util.FunUtil.Sink;
import suite.util.To;
import suite.util.Util;

public class Portfolio {

	private int tradeFrequency = 3;
	private int historyWindow = 1024;

	private Statistic stat = new Statistic();
	private TimeSeries ts = new TimeSeries();

	private Hkex hkex = new Hkex();
	private HkexFactBook hkexFactBook = new HkexFactBook();
	private Yahoo yahoo = new Yahoo();

	private Sink<String> log;
	private AssetAllocator allocator = new AllocateAssetMovingAvgMeanReversion(log);

	public interface AssetAllocator {
		public List<Pair<String, Double>> allocate( //
				Map<String, DataSource> dataSourceByStockCode, //
				List<LocalDate> tradeDates, //
				LocalDate backTestDate);
	}

	public Portfolio() {
		log = System.out::println;
	}

	public Portfolio(Sink<String> log) {
		this.log = log;
	}

	public Simulate simulateLatest(float fund0) {
		return new Simulate(fund0, LocalDate.now(), dates -> Arrays.asList(Util.last(dates)));
	}

	public Simulate simulateFromTo(float fund0, LocalDate from, LocalDate to) {
		Fun<List<LocalDate>, List<LocalDate>> datesPred = dates -> Read.from(dates) //
				.filter(date -> true //
						&& from.compareTo(date) <= 0 //
						&& date.compareTo(to) < 0 //
						&& date.toEpochDay() % tradeFrequency == 0) //
				.toList();
		return new Simulate(fund0, from, datesPred);
	}

	public class Simulate {
		public final Account account;
		public final float[] valuations;

		private Simulate(float fund0, LocalDate from, Fun<List<LocalDate>, List<LocalDate>> datesPred) {
			Map<String, DataSource> dataSourceByStockCode = new HashMap<>();
			LocalDate historyFromDate = from.minusYears(1);
			double valuation = fund0;
			Streamlet<Asset> assets = hkexFactBook.queryLeadingCompaniesByMarketCap(from.getYear() - 1);
			// hkex.getCompanies();

			account = Account.fromCash(fund0);

			Map<String, Integer> lotSizeByStockCode = hkex.queryLotSizeByStockCode(assets);
			yahoo.quote(lotSizeByStockCode.keySet()); // pre-fetch quotes

			for (Asset asset : assets) {
				String stockCode = asset.code;
				if (lotSizeByStockCode.containsKey(stockCode))
					try {
						DataSource dataSource = yahoo.dataSourceWithLatestQuote(stockCode).limitAfter(historyFromDate);
						dataSource.validate();
						dataSourceByStockCode.put(stockCode, dataSource);
					} catch (Exception ex) {
						LogUtil.warn(ex.getMessage() + " in " + asset);
					}
			}

			List<LocalDate> tradeDates = Read.from2(dataSourceByStockCode) //
					.concatMap((stockCode, dataSource) -> Read.from(dataSource.dates)) //
					.distinct() //
					.map(FormatUtil::date) //
					.sort(Util::compare) //
					.toList();

			List<LocalDate> dates = datesPred.apply(tradeDates);
			int size = dates.size();
			valuations = new float[size];

			for (int i = 0; i < size; i++) {
				LocalDate date = dates.get(i);
				DatePeriod historyWindowPeriod = DatePeriod.of(date.minusDays(historyWindow), date);

				Map<String, DataSource> backTestDataSourceByStockCode = Read.from2(dataSourceByStockCode) //
						.mapValue(dataSource -> dataSource.limit(historyWindowPeriod)) //
						.filterValue(dataSource -> 128 <= dataSource.dates.length) //
						.toMap();

				Map<String, Float> latestPriceByStockCode = Read.from2(backTestDataSourceByStockCode) //
						.mapValue(dataSource -> dataSource.last().price) //
						.toMap();

				List<Pair<String, Double>> potentialStatsByStockCode = allocator.allocate( //
						backTestDataSourceByStockCode, //
						tradeDates, //
						date);

				double totalPotential = Read.from2(potentialStatsByStockCode) //
						.collect(As.<String, Double> sumOfDoubles((stockCode, potential) -> potential));

				double valuation_ = valuation;

				Map<String, Integer> portfolio = Read.from2(potentialStatsByStockCode) //
						.map2((stockCode, potential) -> stockCode, (stockCode, potential) -> {
							float price = backTestDataSourceByStockCode.get(stockCode).last().price;
							int lotSize = lotSizeByStockCode.get(stockCode);
							double lots = valuation_ * potential / (totalPotential * price * lotSize);
							return lotSize * (int) lots; // truncate
							// return lotSize * Math.round(lots);
						}) //
						.toMap();

				String actions = account.switchPortfolio(portfolio, latestPriceByStockCode);
				account.validate();

				valuations[i] = (float) (valuation = account.valuation(latestPriceByStockCode));

				log.sink(FormatUtil.formatDate(date) //
						+ ", valuation = " + valuation //
						+ ", portfolio = " + TradeUtil.format(portfolio) //
						+ ", actions = " + actions);
			}

			LocalDate date0 = Util.first(dates);
			LocalDate datex = Util.last(dates);
			double v0 = valuations[0];
			double vx = valuations[valuations.length - 1];

			double nYears = DatePeriod.of(date0, datex).nYears();
			double annualReturn = Math.expm1(Math.log(vx / v0) / nYears);
			double sharpe = ts.sharpeRatio(valuations, nYears);
			double skewness = stat.skewness(valuations);

			log.sink("annual return = " + To.string(annualReturn) //
					+ ", sharpe = " + To.string(sharpe) //
					+ ", skewness = " + To.string(skewness));
		}
	}

}
