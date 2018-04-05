package suite.trade.backalloc;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import suite.adt.pair.Pair;
import suite.math.numeric.Statistic;
import suite.os.LogUtil;
import suite.primitive.Ints_;
import suite.streamlet.Read;
import suite.streamlet.Streamlet;
import suite.trade.Account;
import suite.trade.Asset;
import suite.trade.Time;
import suite.trade.TimeRange;
import suite.trade.Trade;
import suite.trade.Trade_;
import suite.trade.Trade_.UpdatePortfolio;
import suite.trade.backalloc.BackAllocator.OnDateTime;
import suite.trade.data.Configuration;
import suite.trade.data.DataSource.AlignKeyDataSource;
import suite.trade.data.DataSource.Eod;
import suite.util.FunUtil.Sink;
import suite.util.To;
import ts.TimeSeries;

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
			var symbols = assetBySymbol.keySet();
			TimeRange historyPeriod = TimeRange.of(period.from.addYears(-1), period.to);

			AlignKeyDataSource<String> akds = cfg.dataSources(historyPeriod, Read.from(symbols));
			var dsBySymbol = akds.dsByKey;
			long[] tradeTs = akds.ts;
			var t0 = period.from.epochSec();
			var tx = period.to.epochSec();

			var indices = Ints_ //
					.range(tradeTs.length) //
					.filter(i -> {
						var t = tradeTs[i];
						return t0 <= t && t < tx;
					}) //
					.toArray();

			var size = indices.length;

			OnDateTime onDateTime = backAllocator.allocate(akds, indices);
			Map<String, Eod> eodBySymbol = Map.ofEntries();
			var valuations_ = new float[size];
			String ymd = null;
			Exception exception_;

			try {
				for (var i = 0; i < size; i++) {
					var index = indices[i];
					var time = Time.ofEpochSec(tradeTs[index]);

					ymd = time.ymd();
					eodBySymbol = dsBySymbol.mapValue(ds -> ds.getEod(index)).toMap();

					List<Pair<String, Double>> ratioBySymbol = onDateTime.onDateTime(index + 1);
					UpdatePortfolio up = Trade_.updatePortfolio(ymd, account, ratioBySymbol, assetBySymbol, eodBySymbol);
					var valuation_ = valuations_[i] = up.valuation0;

					for (Pair<String, Float> e : up.val0.streamlet())
						holdBySymbol_.compute(e.t0, (s, h) -> e.t1 / (valuation_ * size) + (h != null ? h : 0d));

					var actions = play(up.trades);

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

			var rs = ts.returnsStatDailyAnnualized(valuations_);

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
			var sb = new StringBuilder();
			var length = valuations.length;

			for (Pair<String, Double> e : Read.from2(holdBySymbol).sortBy((symbol, value) -> -value).take(5))
				sb.append(e.t0 + ":" + String.format("%.0f", e.t1 * 100d) + "%,");

			if (exception == null)
				return period //
						+ " val:" + (0 < length ? valuations[length - 1] : "N/A") //
						+ " yearRtn:" + To.string(annualReturn) //
						+ " sharpe:" + To.string(sharpe) //
						+ " skew:" + To.string(skewness) //
						+ " " + account.transactionSummary(cfg::transactionFee).out0() //
						+ " holds::" + sb + "...";
			else {
				LogUtil.error(exception);
				return "exception = " + exception;
			}
		}
	}

}
