package suite.trade.assetalloc;

import static org.junit.Assert.assertTrue;

import org.junit.Test;

import suite.Constants;
import suite.adt.pair.Pair;
import suite.streamlet.As;
import suite.streamlet.Read;
import suite.streamlet.Streamlet;
import suite.trade.Asset;
import suite.trade.DatePeriod;
import suite.trade.Trade_;
import suite.trade.assetalloc.AssetAllocBackTest.Simulate;
import suite.trade.data.Configuration;
import suite.trade.data.ConfigurationImpl;
import suite.trade.data.Summarize;
import suite.util.FunUtil.Sink;
import suite.util.Object_;
import suite.util.String_;

public class AssetAllocBackTestTest {

	private float initial = 1000000f;
	private DatePeriod period = DatePeriod.ofYear(2016);

	private Sink<String> log = System.out::println;
	private Configuration cfg = new ConfigurationImpl();

	@Test
	public void testBackTest() {
		AssetAllocator assetAllocator = MovingAvgMeanReversionAssetAllocator0.of(cfg, log);
		Simulate sim = backTest(assetAllocator, period);
		Summarize.of(cfg, Read.from(sim.trades)).out(System.out::println, trade -> trade.symbol);
		assertGrowth(sim);
	}

	@Test
	public void testBackTestSingle() {
		Asset asset = cfg.queryCompany("0945.HK");
		AssetAllocator assetAllocator = MovingAvgMeanReversionAssetAllocator.of(cfg, log);
		assertGrowth(backTest(assetAllocator, Read.each(asset), period));
	}

	@Test
	public void testBackTestHsi() {
		String symbol = "^HSI";
		Asset asset = Asset.of(symbol, "Hang Seng Index", 1);
		AssetAllocator assetAllocator = new SingleAssetAllocator(symbol);
		assertGrowth(backTest(assetAllocator, Read.each(asset), period));
	}

	@Test
	public void testControlledExperiment() {
		String results = Read.each(Boolean.FALSE, Boolean.TRUE) //
				.join2(Read.range(2008, 2018).map(DatePeriod::ofYear)) //
				.map((b, period) -> {
					AssetAllocator assetAllocator = MovingAvgMeanReversionAssetAllocator0.of(cfg, log);
					Constants.testFlag = b;
					return "\nTEST = " + b + ", " + backTest(assetAllocator, period).conclusion();
				}) //
				.sort(Object_::compare) //
				.collect(As.joined());
		System.out.println(results);
	}

	@Test
	public void testPairsTrading() {
		Pair<Asset, Asset> pairs0 = Pair.of(cfg.queryCompany("0341.HK"), cfg.queryCompany("0052.HK"));
		Pair<Asset, Asset> pairs1 = Pair.of(cfg.queryCompany("0005.HK"), cfg.queryCompany("2888.HK"));

		String results = Read.each(pairs0, pairs1) //
				.join2(Read.range(2008, 2018).map(DatePeriod::ofYear)) //
				.map((pair, period) -> {
					Asset asset0 = pair.t0;
					Asset asset1 = pair.t1;
					AssetAllocator assetAllocator = //
							AssetAllocator_.filterAssets( //
									symbol -> String_.equals(symbol, asset1.symbol), //
									IndexRelativeAssetAllocator.of( //
											cfg, //
											asset0.symbol, //
											RsiAssetAllocator.of()));
					return "\nTEST = " + pair + ", " + backTest(assetAllocator, Read.each(asset0, asset1), period).conclusion();
				}) //
				.sort(Object_::compare) //
				.collect(As.joined());
		System.out.println(results);
	}

	private Simulate backTest(AssetAllocator assetAllocator, DatePeriod period) {
		Streamlet<Asset> assets = cfg.queryLeadingCompaniesByMarketCap(period.from); // hkex.getCompanies()
		return backTest(assetAllocator, assets, period);
	}

	private Simulate backTest(AssetAllocator assetAllocator, Streamlet<Asset> assets, DatePeriod period) {
		AssetAllocBackTest backTest = AssetAllocBackTest.ofFromTo( //
				cfg, //
				assets, //
				assetAllocator, //
				period, //
				log);

		Simulate sim = backTest.simulate(initial);

		System.out.println(sim.conclusion());
		System.out.println(sim.account.transactionSummary(cfg::transactionFee));
		return sim;
	}

	private void assertGrowth(Simulate sim) {
		float[] valuations = sim.valuations;
		int last = valuations.length - 1;
		double r = Trade_.riskFreeInterestRate(last);
		assertTrue(initial * r < valuations[last]);
	}

}
