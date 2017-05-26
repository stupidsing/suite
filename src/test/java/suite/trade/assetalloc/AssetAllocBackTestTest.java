package suite.trade.assetalloc;

import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.List;

import org.junit.Test;

import suite.Constants;
import suite.DailyMain;
import suite.adt.pair.Pair;
import suite.math.stat.Statistic;
import suite.streamlet.As;
import suite.streamlet.Read;
import suite.streamlet.Streamlet;
import suite.streamlet.Streamlet2;
import suite.trade.Asset;
import suite.trade.DatePeriod;
import suite.trade.Trade_;
import suite.trade.analysis.Summarize;
import suite.trade.assetalloc.AssetAllocBackTest.Simulate;
import suite.trade.data.Configuration;
import suite.trade.data.ConfigurationImpl;
import suite.util.FunUtil.Sink;
import suite.util.Object_;
import suite.util.To;

public class AssetAllocBackTestTest {

	private float initial = 1000000f;
	private DatePeriod period = DatePeriod.ofYear(2017);

	private Sink<String> log = System.out::println;
	private Configuration cfg = new ConfigurationImpl();
	private Statistic stat = new Statistic();

	private String hsiSymbol = "^HSI";
	private Asset hsi = Asset.of(hsiSymbol, "Hang Seng Index", 1);

	@Test
	public void testBackTest() {
		AssetAllocator assetAllocator = MovingAvgMeanReversionAssetAllocator0.of(log);
		Simulate sim = backTest(assetAllocator, period);
		Summarize.of(cfg, Read.from(sim.trades)).out(System.out::println, trade -> trade.symbol);
		assertGrowth(out(sim));
	}

	@Test
	public void testBackTestSingle() {
		Asset asset = cfg.queryCompany("0945.HK");
		AssetAllocator assetAllocator = MovingAvgMeanReversionAssetAllocator.of(log);
		assertGrowth(out(backTest(assetAllocator, period, Read.each(asset))));
	}

	@Test
	public void testControlledExperiment() {
		Streamlet2<Boolean, Simulate> simulationsByKey = Read //
				.each(Boolean.FALSE, Boolean.TRUE) //
				.join2(Read.range(2008, 2018).map(DatePeriod::ofYear)) //
				.map2((key, period) -> {
					AssetAllocator assetAllocator = key //
							? new DailyMain().aa_bb //
							: AssetAllocator_.ofSingle(hsiSymbol);

					Constants.testFlag = key;
					return backTest(assetAllocator, period);
				}) //
				.collect(As::streamlet2);

		System.out.println(conclude(simulationsByKey));
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
					AssetAllocator assetAllocator = AssetAllocator_.byPairs(cfg, asset0, asset1);
					return backTest(assetAllocator, period, Read.each(asset0, asset1));
				}) //
				.collect(As::streamlet2);

		System.out.println(conclude(simulationsByKey));
	}

	private <T> String conclude(Streamlet2<T, Simulate> simulationsByKey) {
		Streamlet<String> results0 = simulationsByKey //
				.map((key, simulate) -> "\nTEST = " + key + ", " + simulate.conclusion());

		Streamlet<String> results1 = simulationsByKey //
				.groupBy(sims -> stat.meanVariance(sims.collect(As.arrayOfFloats(sim -> (float) sim.annualReturn)))) //
				.map((key, mv) -> {
					double apr = mv.mean;
					double sd = mv.standardDeviation();
					return "\nTEST = " + key //
							+ ">> apr = " + To.string(apr) //
							+ ", std dev = " + To.string(sd) //
							+ ", sharpe = " + To.string(apr / sd);
				});

		return Streamlet.concat(results0, results1).sort(Object_::compare).collect(As.joined());
	}

	private Simulate backTest(AssetAllocator assetAllocator, DatePeriod period) {
		Streamlet<Asset> assets0 = cfg.queryLeadingCompaniesByMarketCap(period.from); // hkex.getCompanies()
		Streamlet<Asset> assets1 = assets0.cons(hsi);
		return backTest(assetAllocator, period, assets1);
	}

	private Simulate backTest(AssetAllocator assetAllocator, DatePeriod period, Streamlet<Asset> assets) {
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
