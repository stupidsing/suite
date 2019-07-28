package suite.trade.walkforwardalloc;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import suite.math.numeric.Statistic;
import suite.primitive.Floats.FloatsBuilder;
import suite.primitive.Floats_;
import suite.primitive.Longs_;
import suite.streamlet.FunUtil.Sink;
import suite.streamlet.Read;
import suite.streamlet.Streamlet;
import suite.trade.Account;
import suite.trade.Instrument;
import suite.trade.Time;
import suite.trade.Trade;
import suite.trade.Trade_;
import suite.trade.data.DataSource;
import suite.trade.data.DataSource.AlignKeyDataSource;
import suite.trade.data.DataSource.Eod;
import suite.trade.data.TradeCfg;
import suite.ts.TimeSeries;
import suite.util.String_;
import suite.util.To;

public class WalkForwardAllocTester {

	private int windowSize = 360;
	private TradeCfg cfg;
	private Statistic stat = new Statistic();
	private TimeSeries ts = new TimeSeries();
	private WalkForwardAllocator wfa;
	private Sink<String> log;

	private long start;
	private Map<String, Instrument> instrumentBySymbol;
	private Map<String, DataSource> dsBySymbol;

	private long[] times;
	private Account account;
	private FloatsBuilder valuations;
	private List<Trade> trades;
	private Map<String, Double> holdBySymbol;

	public WalkForwardAllocTester(TradeCfg cfg, Streamlet<Instrument> instruments, float fund0, WalkForwardAllocator wfa) {
		this.cfg = cfg;
		this.wfa = wfa;
		this.log = System.out::println;

		times = new long[windowSize];
		account = Account.ofCash(fund0);
		valuations = new FloatsBuilder();
		trades = new ArrayList<>();
		holdBySymbol = new HashMap<>();

		start = System.currentTimeMillis();
		instrumentBySymbol = instruments.toMap(instrument -> instrument.symbol);

		dsBySymbol = instruments //
				.map2(instrument -> instrument.symbol, instrument -> DataSource.of(times, new float[windowSize])) //
				.toMap();
	}

	public String tick() {
		var time = Time.now();
		var priceBySymbol = cfg.quote(dsBySymbol.keySet());

		for (var e : priceBySymbol.entrySet())
			log.f(time.ymdHms() + "," + e.getKey() + "," + e.getValue());

		return tick(time, priceBySymbol);
	}

	public String tick(Time time, Map<String, Float> priceBySymbol) {
		var last = windowSize - 1;

		Longs_.copy(times, 0, times, 1, last);
		times[last] = time.epochSec();

		for (var e : dsBySymbol.entrySet()) {
			var symbol = e.getKey();
			var prices = e.getValue().prices;
			Floats_.copy(prices, 0, prices, 1, last);
			prices[last] = priceBySymbol.get(symbol);
		}

		var akds = new AlignKeyDataSource<String>(times, Read.from2(dsBySymbol));
		var ratioBySymbol = wfa.allocate(akds, windowSize);

		var up = Trade_.updatePortfolio(time.ymdHms(), account, ratioBySymbol, instrumentBySymbol,
				Read.from2(priceBySymbol).mapValue(Eod::of).toMap());

		float valuation_;

		valuations.append(valuation_ = up.valuation0);

		for (var e : up.val0.streamlet())
			holdBySymbol.compute(e.k, (s, h) -> e.v / valuation_ + (h != null ? h : 0d));

		var trades_ = up.trades;
		String actions;

		if (windowSize <= valuations.size())
			actions = play(trades_);
		else
			actions = "wait";

		return time.ymdHms() //
				+ ", valuation = " + valuation_ //
				+ ", portfolio = " + account //
				+ ", actions = " + actions;
	}

	public String conclusion() {
		var valuations_ = valuations.toFloats().toArray();
		var length = valuations_.length;
		var deltaMs = (start - System.currentTimeMillis()) / length;
		var rs = ts.returnsStat(valuations_, deltaMs);

		var holds = String_.build(sb -> {
			for (var e : Read.from2(holdBySymbol).sortBy((symbol, value) -> -value).take(5))
				sb.append(e.<String> map((symbol, hold) -> symbol + ":" + To.percent(hold / length) + ","));
		});

		return "nTicks:" + length //
				+ " val:" + (0 < length ? valuations_[length - 1] : "N/A") //
				+ " tickRtn:" + To.string(rs.return_) //
				+ " sharpe:" + To.string(rs.sharpeRatio()) //
				+ " skew:" + To.string(stat.skewness(valuations_)) //
				+ " " + account.transactionSummary(cfg::transactionFee).out0() //
				+ " holds:" + holds + "...";
	}

	private String play(List<Trade> trades_) {
		trades.addAll(trades_);
		return account.playValidate(trades_);
	}

}
