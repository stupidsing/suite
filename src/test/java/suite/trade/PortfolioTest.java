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
		LocalDate frDate = LocalDate.of(2013, 1, 1);
		LocalDate toDate = LocalDate.of(2020, 1, 1);
		assertTrue(1.05f < portfolio.simulateFromTo(1000000f, frDate, toDate));
	}

}
