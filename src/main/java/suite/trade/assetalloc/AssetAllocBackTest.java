package suite.trade.assetalloc;

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
import suite.trade.Account;
import suite.trade.Asset;
import suite.trade.DatePeriod;
import suite.trade.data.Configuration;
import suite.trade.data.DataSource;
import suite.util.FormatUtil;
import suite.util.FunUtil.Fun;
import suite.util.FunUtil.Sink;
import suite.util.To;
import suite.util.Util;

public class AssetAllocBackTest {

	private int historyWindow = 1024;

	private Configuration cfg = new Configuration();
	private Statistic stat = new Statistic();
	private TimeSeries ts = new TimeSeries();

	private AssetAllocator assetAllocator;
	private Sink<String> log;

	public AssetAllocBackTest(AssetAllocator assetAllocator) {
		this(assetAllocator, System.out::println);
	}

	public AssetAllocBackTest(AssetAllocator assetAllocator, Sink<String> log) {
		this.assetAllocator = assetAllocator;
		this.log = log;
	}

	public Simulate simulateLatest(float fund0) {
		return new Simulate(fund0, LocalDate.now(), dates -> Arrays.asList(Util.last(dates)));
	}

	public Simulate simulateFromTo(float fund0, DatePeriod period) {
		Fun<List<LocalDate>, List<LocalDate>> datesPred = dates -> Read.from(dates) //
				.filter(period::contains) //
				.toList();
		return new Simulate(fund0, period.from, datesPred);
	}

	public class Simulate {
		public final Account account;
		public final float[] valuations;
		public final double annualReturn;
		public final double sharpe;
		public final double skewness;

		private Simulate(float fund0, LocalDate from, Fun<List<LocalDate>, List<LocalDate>> datesPred) {
			Map<String, DataSource> dataSourceBySymbol = new HashMap<>();
			LocalDate historyFromDate = from.minusYears(1);
			double valuation = fund0;
			Streamlet<Asset> assets = cfg.queryLeadingCompaniesByMarketCap(from.getYear() - 1);
			// hkex.getCompanies();

			account = Account.fromCash(fund0);

			Map<String, Integer> lotSizeBySymbol = cfg.queryLotSizeByAsset(assets);

			// pre-fetch quotes
			cfg.quote(lotSizeBySymbol.keySet());

			for (Asset asset : assets) {
				String symbol = asset.symbol;
				if (lotSizeBySymbol.containsKey(symbol))
					try {
						DataSource dataSource = cfg.dataSourceWithLatestQuote(symbol).after(historyFromDate);
						dataSource.validate();
						dataSourceBySymbol.put(symbol, dataSource);
					} catch (Exception ex) {
						LogUtil.warn(ex.getMessage() + " in " + asset);
					}
			}

			List<LocalDate> tradeDates = Read.from2(dataSourceBySymbol) //
					.concatMap((symbol, dataSource) -> Read.from(dataSource.dates)) //
					.distinct() //
					.map(FormatUtil::date) //
					.sort(Util::compare) //
					.toList();

			List<LocalDate> dates = datesPred.apply(tradeDates);
			int size = dates.size();
			String actions;

			valuations = new float[size];

			for (int i = 0; i < size; i++) {
				LocalDate date = dates.get(i);
				DatePeriod historyWindowPeriod = DatePeriod.of(date.minusDays(historyWindow), date);

				Map<String, DataSource> backTestDataSourceBySymbol = Read.from2(dataSourceBySymbol) //
						.mapValue(dataSource -> dataSource.range(historyWindowPeriod)) //
						.filterValue(dataSource -> 128 <= dataSource.dates.length) //
						.toMap();

				Map<String, Float> latestPriceBySymbol = Read.from2(backTestDataSourceBySymbol) //
						.mapValue(dataSource -> dataSource.last().price) //
						.toMap();

				List<Pair<String, Double>> potentialStatsBySymbol = assetAllocator.allocate( //
						backTestDataSourceBySymbol, //
						tradeDates, //
						date);

				if (potentialStatsBySymbol != null) {
					double totalPotential = Read.from2(potentialStatsBySymbol) //
							.collect(As.<String, Double> sumOfDoubles((symbol, potential) -> potential));

					double valuation_ = valuation;

					Map<String, Integer> portfolio = Read.from2(potentialStatsBySymbol) //
							.filterKey(symbol -> !Util.stringEquals(symbol, Asset.cashCode)) //
							.map2((symbol, potential) -> symbol, (symbol, potential) -> {
								float price = backTestDataSourceBySymbol.get(symbol).last().price;
								int lotSize = lotSizeBySymbol.get(symbol);
								double lots = valuation_ * potential / (totalPotential * price * lotSize);
								return lotSize * (int) lots; // truncate
								// return lotSize * Math.round(lots);
							}) //
							.toMap();

					actions = account.switchPortfolio(portfolio, latestPriceBySymbol);
				} else
					actions = null;

				account.validate();

				valuations[i] = (float) (valuation = account.valuation(latestPriceBySymbol));

				log.sink(FormatUtil.formatDate(date) //
						+ ", valuation = " + valuation //
						+ ", portfolio = " + account //
						+ ", actions = " + actions);
			}

			LocalDate date0 = Util.first(dates);
			LocalDate datex = Util.last(dates);
			double v0 = valuations[0];
			double vx = valuations[valuations.length - 1];
			double nYears = DatePeriod.of(date0, datex).nYears();

			annualReturn = Math.expm1(Math.log(vx / v0) / nYears);
			sharpe = ts.sharpeRatio(valuations, nYears);
			skewness = stat.skewness(valuations);
		}

		public String conclusion() {
			return "annual return = " + To.string(annualReturn) //
					+ ", sharpe = " + To.string(sharpe) //
					+ ", skewness = " + To.string(skewness);
		}
	}

}
