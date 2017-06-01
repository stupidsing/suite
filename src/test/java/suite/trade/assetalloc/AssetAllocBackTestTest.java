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
import suite.streamlet.Streamlet2;
import suite.trade.Asset;
import suite.trade.BackTestRunner;
import suite.trade.DatePeriod;
import suite.trade.Trade_;
import suite.trade.analysis.Summarize;
import suite.trade.assetalloc.AssetAllocBackTest.Simulate;
import suite.trade.data.Configuration;
import suite.trade.data.ConfigurationImpl;
import suite.util.FunUtil.Sink;

public class AssetAllocBackTestTest {

	private float initial = 1000000f;
	private DatePeriod period = DatePeriod.ofYear(2017);

	private Sink<String> log = System.out::println;
	private Configuration cfg = new ConfigurationImpl();
	private BackTestRunner runner = new BackTestRunner();

	@Test
	public void testBackTest() {
		AssetAllocator assetAllocator = MovingAvgMeanReversionAssetAllocator0.of(log);
		Simulate sim = runner.backTest(assetAllocator, period);
		Summarize.of(cfg, Read.from(sim.trades)).out(System.out::println, trade -> trade.symbol);
		assertGrowth(out(sim));
	}

	@Test
	public void testBackTestSingle() {
		Asset asset = cfg.queryCompany("0945.HK");
		AssetAllocator assetAllocator = MovingAvgMeanReversionAssetAllocator.of(log);
		assertGrowth(out(runner.backTest(assetAllocator, period, Read.each(asset))));
	}

	@Test
	public void testControlledExperiment() {
		String symbol0 = "0945.HK";
		String symbol1 = "1299.HK";
		Streamlet<Asset> assets = Read.each(symbol0, symbol1).map(cfg::queryCompany).collect(As::streamlet);

		Streamlet2<Boolean, Simulate> simulationsByKey = Read //
				.each(Boolean.FALSE, Boolean.TRUE) //
				.join2(Read.range(2008, 2018).map(DatePeriod::ofYear)) //
				.map2((key, period) -> {
					AssetAllocator assetAllocator = key //
							? AssetAllocator_.bollingerBands1() //
							: AssetAllocator_.ofSingle("^HSI");

					Constants.testFlag = key;
					return runner.backTest(assetAllocator, period, assets);
				}) //
				.collect(As::streamlet2);

		System.out.println(runner.conclude(simulationsByKey));
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

		Streamlet2<Pair<Asset, Asset>, Simulate> simulationsByKey = Read //
				.from(assetPairs) //
				.join2(Read.range(2008, 2018).map(DatePeriod::ofYear)) //
				.map2((pair, period) -> {
					Asset asset0 = pair.t0;
					Asset asset1 = pair.t1;
					AssetAllocator assetAllocator = AssetAllocator_.byPairs(cfg, asset0.symbol, asset1.symbol);
					return runner.backTest(assetAllocator, period, Read.each(asset0, asset1));
				}) //
				.collect(As::streamlet2);

		System.out.println(runner.conclude(simulationsByKey));
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
