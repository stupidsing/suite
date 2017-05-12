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
import suite.util.String_;
import suite.util.To;
import suite.util.Util;

public class AssetAllocBackTest {

	private int historyWindow = 1024;

	private Configuration cfg = new Configuration();
	private Statistic stat = new Statistic();
	private TimeSeries ts = new TimeSeries();

	private Streamlet<Asset> assets;
	private AssetAllocator assetAllocator;
	private LocalDate historyFromDate;
	private Fun<List<LocalDate>, List<LocalDate>> datesPred;
	private Sink<String> log;

	public static AssetAllocBackTest of( //
			Configuration cfg, //
			Streamlet<Asset> assets, //
			AssetAllocator assetAllocator, //
			Sink<String> log) {
		LocalDate historyFromDate = LocalDate.now();
		Fun<List<LocalDate>, List<LocalDate>> datesPred = dates -> Arrays.asList(Util.last(dates));
		return new AssetAllocBackTest(cfg, assets, assetAllocator, historyFromDate, datesPred, log);
	}

	public static AssetAllocBackTest ofFromTo( //
			Configuration cfg, //
			Streamlet<Asset> assets, //
			AssetAllocator assetAllocator, //
			DatePeriod period, //
			Sink<String> log) {
		LocalDate historyFromDate = period.from;
		Fun<List<LocalDate>, List<LocalDate>> datesPred = dates -> Read.from(dates).filter(period::contains).toList();
		return new AssetAllocBackTest(cfg, assets, assetAllocator, historyFromDate, datesPred, log);
	}

	public AssetAllocBackTest( //
			Configuration cfg, //
			Streamlet<Asset> assets, //
			AssetAllocator assetAllocator, //
			LocalDate from, //
			Fun<List<LocalDate>, List<LocalDate>> datesPred, //
			Sink<String> log) {
		this.cfg = cfg;
		this.assets = assets;
		this.historyFromDate = from.minusYears(1);
		this.assetAllocator = assetAllocator;
		this.datesPred = datesPred;
		this.log = log;
	}

	public Simulate simulate(float fund0) {
		return new Simulate(fund0);
	}

	public class Simulate {
		public final Account account;
		public final float[] valuations;
		public final double annualReturn;
		public final double sharpe;
		public final double skewness;

		private Simulate(float fund0) {
			Map<String, Asset> assetBySymbol = assets.toMap(asset -> asset.symbol);
			Map<String, DataSource> dataSourceBySymbol = new HashMap<>();
			double valuation = fund0;

			account = Account.fromCash(fund0);

			// pre-fetch quotes
			cfg.quote(assetBySymbol.keySet());

			for (String symbol : assetBySymbol.keySet())
				try {
					DataSource dataSource = cfg.dataSourceWithLatestQuote(symbol).after(historyFromDate);
					dataSource.validate();
					dataSourceBySymbol.put(symbol, dataSource);
				} catch (Exception ex) {
					LogUtil.warn(ex.getMessage() + " in " + assetBySymbol.get(symbol));
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

				List<Pair<String, Double>> potentialBySymbol = assetAllocator.allocate( //
						backTestDataSourceBySymbol, //
						tradeDates, //
						date);

				if (potentialBySymbol != null) {
					double totalPotential = Read.from2(potentialBySymbol) //
							.collect(As.<String, Double> sumOfDoubles((symbol, potential) -> potential));

					double valuation_ = valuation;

					Map<String, Integer> portfolio = Read.from2(potentialBySymbol) //
							.filterKey(symbol -> !String_.equals(symbol, Asset.cashCode)) //
							.map2((symbol, potential) -> symbol, (symbol, potential) -> {
								float price = backTestDataSourceBySymbol.get(symbol).last().price;
								int lotSize = assetBySymbol.get(symbol).lotSize;
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
			sharpe = ts.returns(valuations, nYears).sharpeRatio();
			skewness = stat.skewness(valuations);
		}

		public String conclusion() {
			return "annual return = " + To.string(annualReturn) //
					+ ", sharpe = " + To.string(sharpe) //
					+ ", skewness = " + To.string(skewness);
		}
	}

}
