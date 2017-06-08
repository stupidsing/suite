package suite.trade.walkforwardalloc;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import suite.adt.pair.Pair;
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
import suite.util.Copy;
import suite.util.To;

public class WalkForwardAllocTester {

	private int dataSize = 360;
	private Configuration cfg;
	private WalkForwardAllocator wfa;

	private Map<String, Asset> assetBySymbol;
	private Map<String, DataSource> dataSourceBySymbol;

	private String[] times;
	private Account account;
	private List<Trade> trades;
	private Map<String, Double> holdBySymbol;

	public WalkForwardAllocTester(Configuration cfg, Streamlet<Asset> assets, float fund0, WalkForwardAllocator wfa) {
		this.cfg = cfg;
		this.wfa = wfa;

		account = Account.fromCash(fund0);
		times = new String[dataSize];
		assetBySymbol = assets.toMap(asset -> asset.symbol);
		dataSourceBySymbol = assets.map2(asset -> asset.symbol, asset -> new DataSource(times, new float[dataSize])).toMap();
		trades = new ArrayList<>();
	}

	public String tick() {
		Map<String, Float> latestPriceBySymbol = cfg.quote(dataSourceBySymbol.keySet());
		int last = dataSize - 1;

		for (int i = 0; i < last; i++)
			times[i] = times[i + 1];

		Time now = Time.now();
		times[last] = now.ymdHms();

		dataSourceBySymbol = Read.from2(dataSourceBySymbol) //
				.map2((symbol, dataSource) -> symbol, (symbol, dataSource) -> {
					float[] prices1 = new float[dataSize];
					Copy.floats(dataSource.prices, 1, prices1, 0, dataSize - 2);
					prices1[last] = latestPriceBySymbol.get(symbol);
					return new DataSource(times, prices1);
				}) //
				.toMap();

		List<Pair<String, Double>> ratioBySymbol = wfa.allocate(Read.from2(dataSourceBySymbol), last);
		UpdatePortfolio up = Trade_.updatePortfolio(account, ratioBySymbol, assetBySymbol, latestPriceBySymbol);
		float valuation_ = up.valuation0;

		for (Pair<String, Float> e : up.val0.stream())
			holdBySymbol.compute(e.t0, (s, h) -> e.t1 / valuation_ + (h != null ? h : 0d));

		List<Trade> trades_ = up.trades;
		String actions = play(trades_);
		trades.addAll(trades_);

		return now.ymd() //
				+ ", valuation = " + valuation_ //
				+ ", portfolio = " + account //
				+ ", actions = " + actions;
	}

	public String hold() {
		StringBuilder sb = new StringBuilder();
		for (Pair<String, Double> e : Read.from2(holdBySymbol).sortBy((symbol, value) -> -value))
			sb.append(e.t0 + ":" + To.string(e.t1) + ",");
		return sb.toString();
	}

	private String play(List<Trade> trades) {
		trades.addAll(trades);
		account.play(trades);
		account.validate();
		return Trade_.format(trades);
	}

}
