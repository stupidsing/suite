package suite.trade;

import static org.junit.Assert.assertTrue;

import java.time.LocalDate;

import org.junit.Test;

public class PortfolioTest {

	private Yahoo yahoo = new Yahoo();

	@Test
	public void testStats() {
		System.out.println(new Portfolio().new MeanReversionStats(yahoo.dataSource("1113.HK")));
	}

	@Test
	public void testPortfolio() {
		float return_ = new Portfolio().simulate(1000000f, date -> LocalDate.of(2013, 1, 1).isBefore(date));
		assertTrue(1.05f < return_);
	}

}
