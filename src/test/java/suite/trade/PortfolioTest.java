package suite.trade;

import static org.junit.Assert.assertTrue;

import java.time.LocalDate;

import org.junit.Test;

import suite.trade.Portfolio.Simulate;

public class PortfolioTest {

	private Portfolio portfolio = new Portfolio();
	private Yahoo yahoo = new Yahoo();

	@Test
	public void testStats() {
		System.out.println(portfolio.new MeanReversionStats(yahoo.dataSource("1113.HK")));
	}

	@Test
	public void testPortfolio() {
		float initial = 1000000f;
		LocalDate frDate = LocalDate.of(2017, 1, 1);
		LocalDate toDate = LocalDate.of(2020, 1, 1);
		Simulate sim = portfolio.simulateFromTo(initial, frDate, toDate);
		float[] valuations = sim.valuations;
		assertTrue(initial * 1.05f < valuations[valuations.length - 1]);
	}

}
