package suite.trade;

import static org.junit.Assert.assertTrue;

import java.time.LocalDate;

import org.junit.Test;

public class PortfolioTest {

	private Portfolio portfolio = new Portfolio();
	private Yahoo yahoo = new Yahoo();

	@Test
	public void testStats() {
		System.out.println(portfolio.new MeanReversionStats(yahoo.dataSource("1113.HK")));
	}

	@Test
	public void testPortfolio() {
		assertTrue(1.05f < portfolio.simulateFrom(1000000f, LocalDate.of(2013, 1, 1)));
	}

}
