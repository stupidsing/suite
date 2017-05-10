package suite.trade.assetalloc;

import static org.junit.Assert.assertTrue;

import java.time.LocalDate;

import org.junit.Test;

import suite.trade.Account;
import suite.trade.DatePeriod;
import suite.trade.assetalloc.AssetAllocBackTest.Simulate;
import suite.trade.data.Configuration;
import suite.util.FunUtil.Sink;

public class AssetAllocBackTestTest {

	private Sink<String> log = System.out::println;
	private Configuration cfg = new Configuration();
	private AssetAllocator assetAllocator = new MovingAvgMeanReversionAssetAllocator(cfg, log);
	private AssetAllocBackTest backTest = new AssetAllocBackTest(assetAllocator);

	@Test
	public void testBackTest() {
		float initial = 1000000f;
		LocalDate frDate = LocalDate.of(2016, 1, 1);
		LocalDate toDate = LocalDate.of(2020, 1, 1);
		Simulate sim = backTest.simulateFromTo(initial, DatePeriod.of(frDate, toDate));

		Account account = sim.account;
		System.out.println(account.transactionSummary(cfg::transactionFee));

		float[] valuations = sim.valuations;
		assertTrue(initial * 1.05f < valuations[valuations.length - 1]);
	}

}
