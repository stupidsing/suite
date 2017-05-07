package suite.trade;

import static org.junit.Assert.assertTrue;

import java.time.LocalDate;

import org.junit.Test;

import suite.trade.Portfolio.Simulate;
import suite.util.To;

public class PortfolioTest {

	private Portfolio portfolio = new Portfolio();
	private Yahoo yahoo = new Yahoo();

	@Test
	public void testStats() {
		DatePeriod period = DatePeriod.backTestDaysBefore(LocalDate.now(), 512, 32);
		System.out.println(new AllocateAsset(s -> {
		}).new MeanReversionStats(yahoo.dataSource("1113.HK"), period));
	}

	@Test
	public void testPortfolio() {
		float initial = 1000000f;
		LocalDate frDate = LocalDate.of(2016, 1, 1);
		LocalDate toDate = LocalDate.of(2020, 1, 1);
		Simulate sim = portfolio.simulateFromTo(initial, frDate, toDate);

		System.out.println("nTransactions = " + sim.account.nTransactions());
		System.out.println("nTransactionAmount = " + To.string(sim.account.nTransactionAmount()));

		float[] valuations = sim.valuations;
		assertTrue(initial * 1.05f < valuations[valuations.length - 1]);
	}

}
