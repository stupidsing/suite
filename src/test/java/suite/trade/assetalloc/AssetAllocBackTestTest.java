package suite.trade.assetalloc;

import static org.junit.Assert.assertTrue;

import java.time.LocalDate;

import org.junit.Test;

import suite.algo.Statistic;
import suite.trade.DatePeriod;
import suite.trade.assetalloc.AssetAllocBackTest.Simulate;
import suite.trade.data.Configuration;
import suite.util.FunUtil.Sink;

public class AssetAllocBackTestTest {

	private float initial = 1000000f;
	private LocalDate frDate = LocalDate.of(2016, 1, 1);
	private LocalDate toDate = LocalDate.of(2020, 1, 1);

	private Sink<String> log = System.out::println;
	private Configuration cfg = new Configuration();
	private Statistic stat = new Statistic();

	@Test
	public void testBackTest() {
		AssetAllocator assetAllocator = new MovingAvgMeanReversionAssetAllocator(cfg, log);
		float[] valuations = backTest(assetAllocator).valuations;
		assertGrowth(valuations);
	}

	@Test
	public void testBackTestHsi() {
		AssetAllocator assetAllocator = new SingleAssetAllocator("HSI");
		float[] valuations = backTest(assetAllocator).valuations;
		assertGrowth(valuations);
	}

	private Simulate backTest(AssetAllocator assetAllocator) {
		AssetAllocBackTest backTest = new AssetAllocBackTest(assetAllocator, System.out::println);
		Simulate sim = backTest.simulateFromTo(initial, DatePeriod.of(frDate, toDate));

		System.out.println(sim.conclusion());
		System.out.println(sim.account.transactionSummary(cfg::transactionFee));
		return sim;
	}

	private void assertGrowth(float[] valuations) {
		double r = Math.expm1(stat.logRiskFreeInterestRate * DatePeriod.of(frDate, toDate).nYears());
		assertTrue(initial * r < valuations[valuations.length - 1]);
	}

}
