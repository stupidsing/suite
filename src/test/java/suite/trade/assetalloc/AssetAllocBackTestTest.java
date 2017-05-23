package suite.trade.assetalloc;

import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.List;

import org.junit.Test;

import suite.Constants;
import suite.adt.pair.Pair;
import suite.streamlet.As;
import suite.streamlet.Read;
import suite.streamlet.Streamlet;
import suite.trade.Asset;
import suite.trade.DatePeriod;
import suite.trade.Trade_;
import suite.trade.analysis.Summarize;
import suite.trade.assetalloc.AssetAllocBackTest.Simulate;
import suite.trade.data.Configuration;
import suite.trade.data.ConfigurationImpl;
import suite.util.FunUtil.Sink;
import suite.util.Object_;

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
		assertGrowth(out(sim));
	}

	@Test
	public void testBackTestSingle() {
		Asset asset = cfg.queryCompany("0945.HK");
		AssetAllocator assetAllocator = MovingAvgMeanReversionAssetAllocator.of(cfg, log);
		assertGrowth(out(backTest(assetAllocator, Read.each(asset), period)));
	}

	@Test
	public void testBackTestHsi() {
		String symbol = "^HSI";
		Asset asset = Asset.of(symbol, "Hang Seng Index", 1);
		AssetAllocator assetAllocator = AssetAllocator_.ofSingle(symbol);
		assertGrowth(out(backTest(assetAllocator, Read.each(asset), period)));
	}

	@Test
	public void testControlledExperiment() {
		String results = Read.each(Boolean.FALSE, Boolean.TRUE) //
				.join2(Read.range(2008, 2018).map(DatePeriod::ofYear)) //
				.map((b, period) -> {
					AssetAllocator assetAllocator = b ? BollingerBandsAssetAllocator.of() : DonchianAssetAllocator.of();
					Constants.testFlag = b;
					try {
						return "\nTEST = " + b + ", " + backTest(assetAllocator, period).conclusion();
					} catch (Exception ex) {
						return "\nexception = " + ex;
					}
				}) //
				.sort(Object_::compare) //
				.collect(As.joined());
		System.out.println(results);
	}

	@Test
	public void testPairsTrading() {
		List<Pair<String, String>> symbolPairs = Arrays.asList( //
				Pair.of("0341.HK", "0052.HK"), //
				Pair.of("0052.HK", "0341.HK"), //
				Pair.of("0005.HK", "2888.HK"));

		List<Pair<Asset, Asset>> assetPairs = Read.from2(symbolPairs) //
				.map((symbol0, symbol1) -> Pair.of(cfg.queryCompany(symbol0), cfg.queryCompany(symbol1))) //
				.toList();

		String results = Read.from(assetPairs) //
				.join2(Read.range(2008, 2018).map(DatePeriod::ofYear)) //
				.map((pair, period) -> {
					Asset asset0 = pair.t0;
					Asset asset1 = pair.t1;
					AssetAllocator assetAllocator = AssetAllocator_.byPairs(cfg, asset0, asset1);
					try {
						return "\nTEST = " + pair + ", " + backTest(assetAllocator, Read.each(asset0, asset1), period).conclusion();
					} catch (Exception ex) {
						return "\nexception = " + ex;
					}
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

		return backTest.simulate(initial);
	}

	private void assertGrowth(Simulate sim) {
		float[] valuations = sim.valuations;
		int last = valuations.length - 1;
		double r = Trade_.riskFreeInterestRate(last);
		assertTrue(initial * r < valuations[last]);
	}

	private Simulate out(Simulate sim) {
		System.out.println(sim.conclusion());
		return sim;
	}

}
