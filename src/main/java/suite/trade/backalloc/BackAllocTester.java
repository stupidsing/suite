package suite.trade.backalloc;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import suite.adt.pair.Pair;
import suite.math.stat.Statistic;
import suite.math.stat.TimeSeries;
import suite.math.stat.TimeSeries.ReturnsStat;
import suite.os.LogUtil;
import suite.primitive.streamlet.LngStreamlet;
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
import suite.trade.Trade_.UpdatePortfolio;
import suite.trade.backalloc.BackAllocator.OnDateTime;
import suite.trade.data.Configuration;
import suite.trade.data.ConfigurationImpl;
import suite.trade.data.DataSource;
import suite.trade.data.DataSource.AlignKeyDataSource;
import suite.util.FunUtil.Fun;
import suite.util.FunUtil.Sink;
import suite.util.To;

public class BackAllocTester {

	private Configuration cfg = new ConfigurationImpl();
	private Statistic stat = new Statistic();
	private TimeSeries ts = new TimeSeries();

	private Streamlet<Asset> assets;
	private BackAllocator backAllocator;
	private TimeRange period;
	private Fun<long[], long[]> tsPred;
	private Sink<String> log;

	public static BackAllocTester ofNow( //
			Configuration cfg, //
			Streamlet<Asset> assets, //
			BackAllocator backAllocator, //
			Sink<String> log) {
		Time now = Time.now();
		Fun<long[], long[]> timesPred = ts_ -> new long[] { ts_[ts_.length - 1], };
		return new BackAllocTester(cfg, assets, backAllocator, TimeRange.of(now, now), timesPred, log);
	}

	public static BackAllocTester ofFromTo( //
			Configuration cfg, //
			Streamlet<Asset> assets, //
			BackAllocator backAllocator, //
			TimeRange period, //
			Sink<String> log) {
		long t0 = period.from.epochSec();
		long tx = period.to.epochSec();
		Fun<long[], long[]> tsPred = ts_ -> LngStreamlet //
				.of(ts_) //
				.filter(t -> t0 <= t && t < tx) //
				.toArray();
		return new BackAllocTester(cfg, assets, backAllocator, period, tsPred, log);
	}

	private BackAllocTester( //
			Configuration cfg, //
			Streamlet<Asset> assets, //
			BackAllocator backAllocator, //
			TimeRange period, //
			Fun<long[], long[]> timesPred, //
			Sink<String> log) {
		this.cfg = cfg;
		this.assets = assets.distinct();
		this.period = period;
		this.backAllocator = backAllocator;
		this.tsPred = timesPred;
		this.log = log;
	}

	public Simulate simulate(float fund0) {
		return new Simulate(fund0);
	}

	public class Simulate {
		public final Account account;
		public final float[] valuations;
		public final List<Trade> trades;
		public final Map<String, Double> holdBySymbol;
		public final double annualReturn;
		public final double sharpe;
		public final double skewness;
		public final Exception exception;

		private Simulate(float fund0) {
			account = Account.fromCash(fund0);
			trades = new ArrayList<>();

			Map<String, Asset> assetBySymbol = assets.toMap(asset -> asset.symbol);
			Map<String, Double> holdBySymbol_ = new HashMap<>();
			Set<String> symbols = assetBySymbol.keySet();
			TimeRange historyPeriod = TimeRange.of(period.from.addYears(-1), period.to);

			// pre-fetch quotes
			cfg.quote(symbols);

			AlignKeyDataSource<String> akds = Read //
					.from(symbols) //
					.map2(symbol -> {
						try {
							DataSource ds = cfg.dataSource(symbol, historyPeriod);
							ds.validate();
							return ds;
						} catch (Exception ex) {
							LogUtil.warn("for " + symbol + " " + ex);
							return null;
						}
					}) //
					.filterValue(ds -> ds != null) //
					.collect(As::streamlet2) //
					.apply(DataSource::alignAll);

			Streamlet2<String, DataSource> dsBySymbol = akds.dsByKey;
			long[] tradeTs = LngStreamlet.of(akds.ts).toArray();
			long[] ts_ = tsPred.apply(tradeTs);
			int size = ts_.length;

			OnDateTime onDateTime = backAllocator.allocate(dsBySymbol, ts_);
			Map<String, Float> latestPriceBySymbol = Collections.emptyMap();
			float[] valuations_ = new float[size];
			Exception exception_;
			int i = 0;
			String ymd = null;

			try {
				while (i < size) {
					long t = ts_[i];
					Time time = Time.ofEpochSec(t);
					int index = Arrays.binarySearch(tradeTs, t);

					ymd = time.ymd();
					latestPriceBySymbol = dsBySymbol.mapValue(ds -> ds.prices[index]).toMap();

					List<Pair<String, Double>> ratioBySymbol = onDateTime.onDateTime(time, index);
					UpdatePortfolio up = Trade_.updatePortfolio(account, ratioBySymbol, assetBySymbol, latestPriceBySymbol);
					float valuation_ = valuations_[i] = up.valuation0;

					for (Pair<String, Float> e : up.val0.stream())
						holdBySymbol_.compute(e.t0, (s, h) -> e.t1 / (valuation_ * size) + (h != null ? h : 0d));

					String actions = play(up.trades);

					log.sink(ymd //
							+ ", valuation = " + valuation_ //
							+ ", portfolio = " + account //
							+ ", actions = " + actions);

					i++;
				}

				exception_ = null;
			} catch (Exception ex) {
				exception_ = new RuntimeException("at " + ymd, ex);
			}

			trades.addAll(Trade_.sellAll(Read.from(trades), latestPriceBySymbol::get).toList());

			ReturnsStat rs = ts.returnsStatDailyAnnualized(valuations_);

			valuations = valuations_;
			holdBySymbol = holdBySymbol_;
			annualReturn = rs.return_;
			sharpe = rs.sharpeRatio();
			skewness = stat.skewness(valuations_);
			exception = exception_;
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

			if (exception == null)
				return "period = " + period //
						+ ", valuation = " + (0 < length ? valuations[length - 1] : "N/A") //
						+ ", annual return = " + To.string(annualReturn) //
						+ ", sharpe = " + To.string(sharpe) //
						+ ", skewness = " + To.string(skewness) //
						+ ", " + account.transactionSummary(cfg::transactionFee) //
						+ ", holds = " + sb;
			else {
				LogUtil.error(exception);
				return "exception = " + exception;
			}
		}
	}

}
