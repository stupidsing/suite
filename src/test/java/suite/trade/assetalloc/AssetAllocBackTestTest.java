package suite.trade.assetalloc;

import static org.junit.Assert.assertTrue;

import java.time.LocalDate;

import org.junit.Test;

import suite.trade.Account;
import suite.trade.DatePeriod;
import suite.trade.assetalloc.AssetAllocBackTest.Simulate;
import suite.trade.data.Configuration;
import suite.util.FunUtil.Sink;
import suite.util.To;

public class AssetAllocBackTestTest {

	private Sink<String> log = System.out::println;
	private Configuration configuration = new Configuration();
	private AssetAllocator assetAllocator = new MovingAvgMeanReversionAssetAllocator(configuration, log);
	private AssetAllocBackTest backTest = new AssetAllocBackTest(assetAllocator);

	@Test
	public void testBackTest() {
		float initial = 1000000f;
		LocalDate frDate = LocalDate.of(2016, 1, 1);
		LocalDate toDate = LocalDate.of(2020, 1, 1);
		Simulate sim = backTest.simulateFromTo(initial, DatePeriod.of(frDate, toDate));

		Account account = sim.account;
		float transactionAmount = account.transactionAmount();
		System.out.println("nTransactions = " + account.nTransactions());
		System.out.println("nTransactionAmount = " + To.string(transactionAmount));
		System.out.println("transactionFee = " + To.string(configuration.transactionFee(transactionAmount)));

		float[] valuations = sim.valuations;
		assertTrue(initial * 1.05f < valuations[valuations.length - 1]);
	}

}
