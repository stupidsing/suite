package suite.trade.backalloc;

import static suite.util.Friends.forInt;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import suite.math.numeric.Statistic;
import suite.os.Log_;
import suite.streamlet.FunUtil.Sink;
import suite.streamlet.Read;
import suite.streamlet.Streamlet;
import suite.trade.Account;
import suite.trade.Instrument;
import suite.trade.Time;
import suite.trade.TimeRange;
import suite.trade.Trade;
import suite.trade.Trade_;
import suite.trade.data.DataSource.Eod;
import suite.trade.data.TradeCfg;
import suite.ts.TimeSeries;
import suite.util.To;

public class BackAllocTester {

	private TradeCfg cfg;
	private TimeRange period;
	private Streamlet<Instrument> instruments;
	private BackAllocator backAllocator;
	private Sink<String> log;

	private Statistic stat = new Statistic();
	private TimeSeries ts = new TimeSeries();

	public static BackAllocTester of( //
			TradeCfg cfg, //
			TimeRange period, //
			Streamlet<Instrument> instruments, //
			BackAllocator backAllocator, //
			Sink<String> log) {
		return new BackAllocTester(cfg, period, instruments.distinct(), backAllocator, log);
	}

	private BackAllocTester( //
			TradeCfg cfg, //
			TimeRange period, //
			Streamlet<Instrument> instruments, //
			BackAllocator backAllocator, //
			Sink<String> log) {
		this.cfg = cfg;
		this.period = period;
		this.instruments = instruments;
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

			var instrumentBySymbol = instruments.toMap(instrument -> instrument.symbol);
			var holdBySymbol_ = new HashMap<String, Double>();
			var symbols = instrumentBySymbol.keySet();
			var historyPeriod = TimeRange.of(period.from.addYears(-1), period.to);

			var akds = cfg.dataSources(historyPeriod, Read.from(symbols));
			var dsBySymbol = akds.dsByKey;
			var tradeTs = akds.ts;
			var t0 = period.from.epochSec();
			var tx = period.to.epochSec();

			var indices = forInt(tradeTs.length) //
					.filter(i -> {
						var t = tradeTs[i];
						return t0 <= t && t < tx;
					}) //
					.toArray();

			var size = indices.length;

			var onDateTime = backAllocator.allocate(akds, indices);
			var eodBySymbol = Map.<String, Eod> ofEntries();
			var valuations_ = new float[size];
			String ymd = null;
			Exception exception_;

			try {
				for (var i = 0; i < size; i++) {
					var index = indices[i];
					var time = Time.ofEpochSec(tradeTs[index]);

					ymd = time.ymd();
					eodBySymbol = dsBySymbol.mapValue(ds -> ds.getEod(index)).toMap();

					var ratioBySymbol = onDateTime.onDateTime(index + 1);
					var up = Trade_.updatePortfolio(ymd, account, ratioBySymbol, instrumentBySymbol, eodBySymbol);
					var valuation_ = valuations_[i] = up.valuation0;

					for (var e : up.val0.streamlet())
						holdBySymbol_.compute(e.t0, (s, h) -> e.t1 / (valuation_ * size) + (h != null ? h : 0d));

					var actions = play(up.trades);

					log.f(ymd //
							+ ", valuation = " + valuation_ //
							+ ", portfolio = " + account //
							+ ", actions = " + actions);
				}

				exception_ = null;
			} catch (Exception ex) {
				exception_ = new RuntimeException("at " + ymd, ex);
			}

			var eodBySymbol_ = eodBySymbol;
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

			for (var e : Read.from2(holdBySymbol).sortBy((symbol, value) -> -value).take(5))
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
				Log_.error(exception);
				return "exception = " + exception;
			}
		}
	}

}
