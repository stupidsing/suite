package suite.trade;

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
		new Portfolio().simulate(1000000f, date -> LocalDate.of(2013, 1, 1).isBefore(date));
	}

}
