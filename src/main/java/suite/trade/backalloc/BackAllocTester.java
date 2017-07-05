package suite.trade.backalloc;

import java.util.ArrayList;
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
import suite.util.FunUtil.Sink;
import suite.util.To;

public class BackAllocTester {

	private Configuration cfg = new ConfigurationImpl();
	private Statistic stat = new Statistic();
	private TimeSeries ts = new TimeSeries();

	private TimeRange period;
	private Streamlet<Asset> assets;
	private BackAllocator backAllocator;
	private Sink<String> log;

	public static BackAllocTester of( //
			Configuration cfg, //
			TimeRange period, //
			Streamlet<Asset> assets, //
			BackAllocator backAllocator, //
			Sink<String> log) {
		return new BackAllocTester(cfg, period, assets.distinct(), backAllocator, log);
	}

	private BackAllocTester( //
			Configuration cfg, //
			TimeRange period, //
			Streamlet<Asset> assets, //
			BackAllocator backAllocator, //
			Sink<String> log) {
		this.cfg = cfg;
		this.period = period;
		this.assets = assets;
		this.backAllocator = backAllocator;
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

			AlignKeyDataSource<String> akds = cfg.dataSources(Read.from(symbols), historyPeriod);
			Streamlet2<String, DataSource> dsBySymbol = akds.dsByKey;
			long[] tradeTs = akds.ts;

			long t0 = period.from.epochSec();
			long tx = period.to.epochSec();
			long[] ts_ = LngStreamlet.of(tradeTs).filter(t -> t0 <= t && t < tx).toArray();
			int size = ts_.length;

			OnDateTime onDateTime = backAllocator.allocate(dsBySymbol, ts_);
			Map<String, Float> latestPriceBySymbol = Collections.emptyMap();
			float[] valuations_ = new float[size];
			int index = 0;
			String ymd = null;
			Exception exception_;

			try {
				for (int i = 0; i < size; i++) {
					long t = ts_[i];
					Time time = Time.ofEpochSec(t);

					while (tradeTs[index] != t)
						index++;

					int index_ = index;

					ymd = time.ymd();
					latestPriceBySymbol = dsBySymbol.mapValue(ds -> ds.prices[index_]).toMap();

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
