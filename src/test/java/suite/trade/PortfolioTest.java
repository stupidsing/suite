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
		float return_ = portfolio.simulate(1000000f, date -> LocalDate.of(2013, 1, 1).isBefore(date));
		assertTrue(1.05f < return_);
	}

}
