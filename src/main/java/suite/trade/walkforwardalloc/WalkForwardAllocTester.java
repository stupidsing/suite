package suite.trade.walkforwardalloc;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import suite.adt.pair.Pair;
import suite.math.stat.Statistic;
import suite.math.stat.TimeSeries;
import suite.math.stat.TimeSeries.ReturnsStat;
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
import suite.util.FunUtil.Sink;
import suite.util.To;

public class WalkForwardAllocTester {

	private int windowSize = 360;
	private Configuration cfg;
	private Statistic stat = new Statistic();
	private TimeSeries ts = new TimeSeries();
	private WalkForwardAllocator wfa;
	private Sink<String> log;

	private long start;
	private Map<String, Asset> assetBySymbol;
	private Map<String, DataSource> dataSourceBySymbol;

	private String[] times;
	private Account account;
	private FloatsBuilder valuations;
	private List<Trade> trades;
	private Map<String, Double> holdBySymbol;

	public WalkForwardAllocTester(Configuration cfg, Streamlet<Asset> assets, float fund0, WalkForwardAllocator wfa) {
		this.cfg = cfg;
		this.wfa = wfa;
		this.log = System.out::println;

		times = new String[windowSize];
		account = Account.fromCash(fund0);
		valuations = new FloatsBuilder();
		trades = new ArrayList<>();
		holdBySymbol = new HashMap<>();

		start = System.currentTimeMillis();
		assetBySymbol = assets.toMap(asset -> asset.symbol);
		dataSourceBySymbol = assets.map2(asset -> asset.symbol, asset -> new DataSource(times, new float[windowSize])).toMap();
	}

	public String tick() {
		String ymdHms = Time.now().ymdHms();
		Map<String, Float> priceBySymbol = cfg.quote(dataSourceBySymbol.keySet());

		for (Entry<String, Float> e : priceBySymbol.entrySet())
			log.sink(ymdHms + "," + e.getKey() + "," + e.getValue());

		return tick(ymdHms, priceBySymbol);
	}

	private String tick(String ymdHms, Map<String, Float> priceBySymbol) {
		int last = windowSize - 1;

		System.arraycopy(times, 0, times, 1, last);
		times[last] = ymdHms;

		dataSourceBySymbol = Read.from2(dataSourceBySymbol) //
				.map2((symbol, dataSource) -> {
					float[] prices = dataSource.prices;
					System.arraycopy(prices, 0, prices, 1, last);
					prices[last] = priceBySymbol.get(symbol);
					return dataSource;
				}) //
				.toMap();

		List<Pair<String, Double>> ratioBySymbol = wfa.allocate(Read.from2(dataSourceBySymbol), windowSize);
		UpdatePortfolio up = Trade_.updatePortfolio(account, ratioBySymbol, assetBySymbol, priceBySymbol);
		float valuation_;

		valuations.append(valuation_ = up.valuation0);

		for (Pair<String, Float> e : up.val0.stream())
			holdBySymbol.compute(e.t0, (s, h) -> e.t1 / valuation_ + (h != null ? h : 0d));

		List<Trade> trades_ = up.trades;
		String actions;

		if (windowSize <= valuations.size()) {
			actions = play(trades_);
			trades.addAll(trades_);
		} else
			actions = "wait";

		return ymdHms //
				+ ", valuation = " + valuation_ //
				+ ", portfolio = " + account //
				+ ", actions = " + actions;
	}

	public String conclusion() {
		float[] valuations_ = valuations.toFloats().toFloatArray();
		int length = valuations_.length;
		double deltaMs = (start - System.currentTimeMillis()) / length;
		ReturnsStat rs = ts.returnsStat(valuations_, deltaMs);
		StringBuilder sb = new StringBuilder();

		for (Pair<String, Double> e : Read.from2(holdBySymbol).sortBy((symbol, value) -> -value))
			sb.append(e.t0 + ":" + To.string(e.t1 / length) + ",");

		return "nTicks = " + length //
				+ ", valuation = " + (0 < length ? valuations_[length - 1] : "N/A") //
				+ ", tick return = " + To.string(rs.return_) //
				+ ", sharpe = " + To.string(rs.sharpeRatio()) //
				+ ", skewness = " + To.string(stat.skewness(valuations_)) //
				+ ", " + account.transactionSummary(cfg::transactionFee) //
				+ ", holds = " + sb;
	}

	private String play(List<Trade> trades) {
		trades.addAll(trades);
		account.play(trades);
		account.validate();
		return Trade_.format(trades);
	}

}
