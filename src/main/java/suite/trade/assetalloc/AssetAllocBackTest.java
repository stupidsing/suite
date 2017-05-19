package suite.trade.assetalloc;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import suite.adt.Pair;
import suite.algo.Statistic;
import suite.math.TimeSeries;
import suite.os.LogUtil;
import suite.streamlet.Read;
import suite.streamlet.Streamlet;
import suite.trade.Account;
import suite.trade.Account.Valuation;
import suite.trade.Asset;
import suite.trade.DatePeriod;
import suite.trade.Trade;
import suite.trade.Trade_;
import suite.trade.data.Configuration;
import suite.trade.data.ConfigurationImpl;
import suite.trade.data.DataSource;
import suite.util.FunUtil.Fun;
import suite.util.FunUtil.Sink;
import suite.util.List_;
import suite.util.Object_;
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

	public static AssetAllocBackTest of( //
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
			Map<String, DataSource> dataSourceBySymbol = new HashMap<>();
			Map<String, Double> holdBySymbol_ = new HashMap<>();
			double valuation = fund0;

			// pre-fetch quotes
			cfg.quote(assetBySymbol.keySet());

			for (String symbol : assetBySymbol.keySet())
				try {
					DataSource dataSource = cfg.dataSourceWithLatestQuote(symbol).after(historyFromDate);
					dataSource.validate();
					dataSourceBySymbol.put(symbol, dataSource);
				} catch (Exception ex) {
					LogUtil.warn("for " + symbol + " " + ex);
				}

			List<LocalDate> tradeDates = Read.from2(dataSourceBySymbol) //
					.concatMap((symbol, dataSource) -> Read.from(dataSource.dates)) //
					.distinct() //
					.map(To::date) //
					.sort(Object_::compare) //
					.toList();

			List<LocalDate> dates = datesPred.apply(tradeDates);
			int size = dates.size();
			float[] valuations_ = new float[size];
			Map<String, Float> latestPriceBySymbol = null;

			for (int i = 0; i < size; i++) {
				LocalDate date = dates.get(i);
				DatePeriod historyWindowPeriod = DatePeriod.daysBefore(date, historyWindow);

				Map<String, DataSource> backTestDataSourceBySymbol = Read.from2(dataSourceBySymbol) //
						.mapValue(dataSource -> dataSource.range(historyWindowPeriod)) //
						.filterValue(dataSource -> 128 <= dataSource.dates.length) //
						.toMap();

				latestPriceBySymbol = Read.from2(backTestDataSourceBySymbol) //
						.mapValue(dataSource -> dataSource.last().price) //
						.toMap();

				List<Pair<String, Double>> ratioBySymbol = assetAllocator.allocate( //
						backTestDataSourceBySymbol, //
						tradeDates, //
						date);

				double valuation_ = valuation;

				Map<String, Integer> portfolio = Read.from2(ratioBySymbol) //
						.filterKey(symbol -> !String_.equals(symbol, Asset.cashCode)) //
						.map2((symbol, potential) -> {
							float price = backTestDataSourceBySymbol.get(symbol).last().price;
							int lotSize = assetBySymbol.get(symbol).lotSize;
							double lots = valuation_ * potential / (price * lotSize);
							return lotSize * (int) lots; // truncate
							// return lotSize * Math.round(lots);
						}) //
						.toMap();

				String actions = play(Trade_.diff(account.assets(), portfolio, latestPriceBySymbol));
				Valuation val = account.valuation(latestPriceBySymbol);

				valuations_[i] = (float) (valuation = val.sum());

				for (Pair<String, Float> e : val.stream())
					holdBySymbol_.compute(e.t0, (s, h) -> e.t1 / (valuation_ * size) + (h != null ? h : 0d));

				log.sink(To.string(date) //
						+ ", valuation = " + valuation //
						+ ", portfolio = " + account //
						+ ", actions = " + actions);
			}

			trades.addAll(Trade_.sellAll(Read.from(trades), latestPriceBySymbol::get).toList());

			double v0 = valuations_[0];
			double vx = valuations_[size - 1];
			DatePeriod period_ = DatePeriod.of(List_.first(dates), List_.last(dates));

			period = period_;
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

			for (Pair<String, Double> e : Read.from2(holdBySymbol).sortBy((symbol, value) -> -value))
				sb.append(e.t0 + ":" + To.string(e.t1) + ",");

			return "period = " + period //
					+ ", valuation = " + valuations[valuations.length - 1] //
					+ ", annual return = " + To.string(annualReturn) //
					+ ", sharpe = " + To.string(sharpe) //
					+ ", skewness = " + To.string(skewness) //
					+ ", holds = " + sb;
		}
	}

}
