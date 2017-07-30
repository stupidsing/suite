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
import suite.primitive.Ints_;
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
import suite.trade.data.DataSource;
import suite.trade.data.DataSource.AlignKeyDataSource;
import suite.trade.data.DataSource.Eod;
import suite.util.FunUtil.Sink;
import suite.util.To;

public class BackAllocTester {

	private Configuration cfg;
	private TimeRange period;
	private Streamlet<Asset> assets;
	private BackAllocator backAllocator;
	private Sink<String> log;

	private Statistic stat = new Statistic();
	private TimeSeries ts = new TimeSeries();

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
			account = Account.ofCash(fund0);
			trades = new ArrayList<>();

			Map<String, Asset> assetBySymbol = assets.toMap(asset -> asset.symbol);
			Map<String, Double> holdBySymbol_ = new HashMap<>();
			Set<String> symbols = assetBySymbol.keySet();
			TimeRange historyPeriod = TimeRange.of(period.from.addYears(-1), period.to);

			AlignKeyDataSource<String> akds = cfg.dataSources(historyPeriod, Read.from(symbols));
			Streamlet2<String, DataSource> dsBySymbol = akds.dsByKey;
			long[] tradeTs = akds.ts;
			long t0 = period.from.epochSec();
			long tx = period.to.epochSec();

			int[] indices = Ints_ //
					.range(tradeTs.length) //
					.filter(i -> {
						long t = tradeTs[i];
						return t0 <= t && t < tx;
					}) //
					.toArray();

			int size = indices.length;

			OnDateTime onDateTime = backAllocator.allocate(akds, indices);
			Map<String, Eod> eodBySymbol = Collections.emptyMap();
			float[] valuations_ = new float[size];
			String ymd = null;
			Exception exception_;

			try {
				for (int i = 0; i < size; i++) {
					int index = indices[i];
					long t = tradeTs[index];
					Time time = Time.ofEpochSec(t);

					ymd = time.ymd();
					eodBySymbol = dsBySymbol.mapValue(ds -> ds.getEod(index)).toMap();

					List<Pair<String, Double>> ratioBySymbol = onDateTime.onDateTime(index + 1);
					UpdatePortfolio up = Trade_.updatePortfolio(ymd, account, ratioBySymbol, assetBySymbol, eodBySymbol);
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

			Map<String, Eod> eodBySymbol_ = eodBySymbol;
			trades.addAll(Trade_.sellAll(ymd, Read.from(trades), symbol -> eodBySymbol_.get(symbol).nextOpen).toList());

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
			return account.playValidate(trades_);
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
