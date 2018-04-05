package suite.trade.walkforwardalloc;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import suite.adt.pair.Pair;
import suite.math.numeric.Statistic;
import suite.primitive.Floats.FloatsBuilder;
import suite.streamlet.Read;
import suite.streamlet.Streamlet;
import suite.trade.Account;
import suite.trade.Asset;
import suite.trade.Time;
import suite.trade.Trade;
import suite.trade.Trade_;
import suite.trade.Trade_.UpdatePortfolio;
import suite.trade.data.Configuration;
import suite.trade.data.DataSource;
import suite.trade.data.DataSource.AlignKeyDataSource;
import suite.trade.data.DataSource.Eod;
import suite.util.FunUtil.Sink;
import suite.util.To;
import ts.TimeSeries;
import ts.TimeSeries.ReturnsStat;

public class WalkForwardAllocTester {

	private int windowSize = 360;
	private Configuration cfg;
	private Statistic stat = new Statistic();
	private TimeSeries ts = new TimeSeries();
	private WalkForwardAllocator wfa;
	private Sink<String> log;

	private long start;
	private Map<String, Asset> assetBySymbol;
	private Map<String, DataSource> dsBySymbol;

	private long[] times;
	private Account account;
	private FloatsBuilder valuations;
	private List<Trade> trades;
	private Map<String, Double> holdBySymbol;

	public WalkForwardAllocTester(Configuration cfg, Streamlet<Asset> assets, float fund0, WalkForwardAllocator wfa) {
		this.cfg = cfg;
		this.wfa = wfa;
		this.log = System.out::println;

		times = new long[windowSize];
		account = Account.ofCash(fund0);
		valuations = new FloatsBuilder();
		trades = new ArrayList<>();
		holdBySymbol = new HashMap<>();

		start = System.currentTimeMillis();
		assetBySymbol = assets.toMap(asset -> asset.symbol);
		dsBySymbol = assets.map2(asset -> asset.symbol, asset -> DataSource.of(times, new float[windowSize])).toMap();
	}

	public String tick() {
		Time time = Time.now();
		Map<String, Float> priceBySymbol = cfg.quote(dsBySymbol.keySet());

		for (var e : priceBySymbol.entrySet())
			log.sink(time.ymdHms() + "," + e.getKey() + "," + e.getValue());

		return tick(time, priceBySymbol);
	}

	public String tick(Time time, Map<String, Float> priceBySymbol) {
		var last = windowSize - 1;

		System.arraycopy(times, 0, times, 1, last);
		times[last] = time.epochSec();

		for (var e : dsBySymbol.entrySet()) {
			var symbol = e.getKey();
			var prices = e.getValue().prices;
			System.arraycopy(prices, 0, prices, 1, last);
			prices[last] = priceBySymbol.get(symbol);
		}

		AlignKeyDataSource<String> akds = new AlignKeyDataSource<>(times, Read.from2(dsBySymbol));
		List<Pair<String, Double>> ratioBySymbol = wfa.allocate(akds, windowSize);

		UpdatePortfolio up = Trade_.updatePortfolio(time.ymdHms(), account, ratioBySymbol, assetBySymbol,
				Read.from2(priceBySymbol).mapValue(Eod::of).toMap());

		float valuation_;

		valuations.append(valuation_ = up.valuation0);

		for (Pair<String, Float> e : up.val0.streamlet())
			holdBySymbol.compute(e.t0, (s, h) -> e.t1 / valuation_ + (h != null ? h : 0d));

		List<Trade> trades_ = up.trades;
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
		ReturnsStat rs = ts.returnsStat(valuations_, deltaMs);
		StringBuilder sb = new StringBuilder();

		for (Pair<String, Double> e : Read.from2(holdBySymbol).sortBy((symbol, value) -> -value).take(5))
			sb.append(e.t0 + ":" + String.format("%.0f", e.t1 * 100d / length) + "%,");

		return "nTicks:" + length //
				+ " val:" + (0 < length ? valuations_[length - 1] : "N/A") //
				+ " tickRtn:" + To.string(rs.return_) //
				+ " sharpe:" + To.string(rs.sharpeRatio()) //
				+ " skew:" + To.string(stat.skewness(valuations_)) //
				+ " " + account.transactionSummary(cfg::transactionFee).out0() //
				+ " holds::" + sb + "...";
	}

	private String play(List<Trade> trades_) {
		trades.addAll(trades_);
		return account.playValidate(trades_);
	}

}
