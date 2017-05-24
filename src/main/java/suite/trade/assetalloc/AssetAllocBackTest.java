package suite.trade.assetalloc;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import suite.adt.pair.Pair;
import suite.math.stat.Statistic;
import suite.math.stat.TimeSeries;
import suite.os.LogUtil;
import suite.streamlet.As;
import suite.streamlet.Read;
import suite.streamlet.Streamlet;
import suite.streamlet.Streamlet2;
import suite.trade.Account;
import suite.trade.Account.Valuation;
import suite.trade.Asset;
import suite.trade.DatePeriod;
import suite.trade.Trade;
import suite.trade.Trade_;
import suite.trade.data.Configuration;
import suite.trade.data.ConfigurationImpl;
import suite.trade.data.DataSource;
import suite.trade.data.DataSource.AlignDataSource;
import suite.util.FunUtil.Fun;
import suite.util.FunUtil.Sink;
import suite.util.List_;
import suite.util.String_;
import suite.util.To;

public class AssetAllocBackTest {

	private int historyWindow = 1024;

	private Configuration cfg = new ConfigurationImpl();
	private Statistic stat = new Statistic();
	private TimeSeries ts = new TimeSeries();

	private Streamlet<Asset> assets;
	private AssetAllocator assetAllocator;
	private LocalDate historyFromDate;
	private Fun<List<LocalDate>, List<LocalDate>> datesPred;
	private Sink<String> log;

	public static AssetAllocBackTest ofNow( //
			Configuration cfg, //
			Streamlet<Asset> assets, //
			AssetAllocator assetAllocator, //
			Sink<String> log) {
		LocalDate historyFromDate = LocalDate.now();
		Fun<List<LocalDate>, List<LocalDate>> datesPred = dates -> Arrays.asList(List_.last(dates));
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

	private AssetAllocBackTest( //
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
		public final DatePeriod period;
		public final float[] valuations;
		public final List<Trade> trades;
		public final Map<String, Double> holdBySymbol;
		public final double annualReturn;
		public final double sharpe;
		public final double skewness;

		private Simulate(float fund0) {
			account = Account.fromCash(fund0);
			trades = new ArrayList<>();

			Map<String, Asset> assetBySymbol = assets.toMap(asset -> asset.symbol);
			Map<String, Double> holdBySymbol_ = new HashMap<>();
			Set<String> symbols = assetBySymbol.keySet();
			double valuation = fund0;

			// pre-fetch quotes
			cfg.quote(symbols);

			Streamlet2<String, DataSource> dataSourceBySymbol0 = Read.from(symbols) //
					.map2(symbol -> cfg.dataSourceWithLatestQuote(symbol).after(historyFromDate)) //
					.map2((symbol, dataSource) -> {
						try {
							dataSource.validate();
							return dataSource;
						} catch (Exception ex) {
							LogUtil.warn("for " + symbol + " " + ex);
							return null;
						}
					}) //
					.filterValue(dataSource -> dataSource != null) //
					.collect(As::streamlet2);

			AlignDataSource alignDataSource = DataSource.alignAll(dataSourceBySymbol0.values());

			Streamlet2<String, DataSource> dataSourceBySymbol1 = dataSourceBySymbol0 //
					.mapValue(alignDataSource::align) //
					.collect(As::streamlet2);

			String[] tradeDateArray = alignDataSource.dates;
			List<LocalDate> dates = datesPred.apply(Read.from(tradeDateArray).map(To::date).toList());
			int size = dates.size();

			float[] valuations_ = new float[size];
			Map<String, Float> latestPriceBySymbol = null;

			for (int i = 0; i < size; i++) {
				LocalDate date = dates.get(i);
				int index = Arrays.binarySearch(tradeDateArray, To.string(date));
				DatePeriod historyWindowPeriod = DatePeriod.daysBefore(date, historyWindow);

				Map<String, DataSource> backTestDataSourceBySymbol = dataSourceBySymbol1 //
						.mapValue(dataSource -> dataSource.range(historyWindowPeriod)) //
						.filterValue(dataSource -> 128 <= dataSource.dates.length) //
						.toMap();

				latestPriceBySymbol = dataSourceBySymbol1 //
						.mapValue(dataSource -> dataSource.prices[index]) //
						.toMap();

				Valuation val = account.valuation(latestPriceBySymbol);
				valuations_[i] = (float) (valuation = val.sum());
				List<Pair<String, Double>> ratioBySymbol = assetAllocator.allocate(backTestDataSourceBySymbol, date, i + 1);

				Map<String, Float> latestPriceBySymbol_ = latestPriceBySymbol;
				double valuation_ = valuation;

				Map<String, Integer> portfolio = Read.from2(ratioBySymbol) //
						.filterKey(symbol -> !String_.equals(symbol, Asset.cashCode)) //
						.map2((symbol, potential) -> {
							float price = latestPriceBySymbol_.get(symbol);
							int lotSize = assetBySymbol.get(symbol).lotSize;
							return lotSize * (int) Math.floor(valuation_ * potential / (price * lotSize));
						}) //
						.toMap();

				String actions = play(Trade_.diff(account.assets(), portfolio, latestPriceBySymbol));

				for (Pair<String, Float> e : val.stream())
					holdBySymbol_.compute(e.t0, (s, h) -> e.t1 / (valuation_ * size) + (h != null ? h : 0d));

				log.sink(To.string(date) //
						+ ", valuation = " + valuation //
						+ ", portfolio = " + account //
						+ ", actions = " + actions);
			}

			trades.addAll(Trade_.sellAll(Read.from(trades), latestPriceBySymbol::get).toList());

			double v0, vx;
			if (0 < size) {
				v0 = valuations_[0];
				vx = valuations_[size - 1];
			} else
				v0 = vx = 1d;

			period = DatePeriod.of(List_.first(dates), List_.last(dates));
			valuations = valuations_;
			holdBySymbol = holdBySymbol_;
			annualReturn = Math.expm1(Math.log(vx / v0) * Trade_.nTradeDaysPerYear / size);
			sharpe = ts.returnsStat(valuations).sharpeRatio();
			skewness = stat.skewness(valuations);
		}

		private String play(List<Trade> trades_) {
			trades.addAll(trades_);
			account.play(trades_);
			account.validate();
			return Trade_.format(trades_);
		}

		public String conclusion() {
			StringBuilder sb = new StringBuilder();
			int length = valuations.length;

			for (Pair<String, Double> e : Read.from2(holdBySymbol).sortBy((symbol, value) -> -value))
				sb.append(e.t0 + ":" + To.string(e.t1) + ",");

			return "period = " + period //
					+ ", valuation = " + (0 < length ? valuations[length - 1] : "N/A") //
					+ ", annual return = " + To.string(annualReturn) //
					+ ", sharpe = " + To.string(sharpe) //
					+ ", skewness = " + To.string(skewness) //
					+ ", " + account.transactionSummary(cfg::transactionFee) //
					+ ", holds = " + sb;
		}
	}

}
