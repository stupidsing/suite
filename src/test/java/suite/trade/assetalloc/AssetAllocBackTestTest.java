package suite.trade.assetalloc;

import static org.junit.Assert.assertTrue;

import java.time.LocalDate;

import org.junit.Test;

import suite.algo.Statistic;
import suite.streamlet.Read;
import suite.streamlet.Streamlet;
import suite.trade.Asset;
import suite.trade.DatePeriod;
import suite.trade.assetalloc.AssetAllocBackTest.Simulate;
import suite.trade.data.Configuration;
import suite.util.FunUtil.Sink;

public class AssetAllocBackTestTest {

	private float initial = 1000000f;
	private LocalDate frDate = LocalDate.of(2016, 1, 1);
	private LocalDate toDate = LocalDate.of(2017, 7, 1);
	private DatePeriod period = DatePeriod.of(frDate, toDate);

	private Sink<String> log = System.out::println;
	private Configuration cfg = new Configuration();
	private Statistic stat = new Statistic();

	@Test
	public void testBackTest() {
		AssetAllocator assetAllocator = MovingAvgMeanReversionAssetAllocator.of(cfg, log);
		assertGrowth(backTest(assetAllocator));
	}

	@Test
	public void testBackTestSingle() {
		Asset asset = cfg.queryCompany("0945.HK");
		AssetAllocator assetAllocator = MovingAvgMeanReversionAssetAllocator.of(cfg, log);
		assertGrowth(backTest(assetAllocator, Read.each(asset)));
	}

	@Test
	public void testBackTestHsi() {
		String symbol = "^HSI";
		Asset asset = Asset.of(symbol, "Hang Seng Index", 1);
		AssetAllocator assetAllocator = new SingleAssetAllocator(symbol);
		assertGrowth(backTest(assetAllocator, Read.each(asset)));
	}

	private Simulate backTest(AssetAllocator assetAllocator) {
		Streamlet<Asset> assets = cfg.queryLeadingCompaniesByMarketCap(frDate); // hkex.getCompanies()
		return backTest(assetAllocator, assets);
	}

	private Simulate backTest(AssetAllocator assetAllocator, Streamlet<Asset> assets) {
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
		double r = Math.expm1(stat.logRiskFreeInterestRate * period.nYears());
		assertTrue(initial * r < valuations[valuations.length - 1]);
	}

}
