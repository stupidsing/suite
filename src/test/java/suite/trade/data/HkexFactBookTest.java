package suite.trade.data;

import static org.junit.Assert.assertTrue;
import static suite.util.Friends.forInt;

import java.util.HashSet;

import org.junit.Test;

import suite.trade.Trade_;
import suite.util.String_;

public class HkexFactBookTest {

	private HkexFactBook hkexFactBook = new HkexFactBook();

	@Test
	public void testDelist() {
		var delisted = new HashSet<String>(hkexFactBook.queryDelisted().toList());

		System.out.println(delisted);
		System.out.println(forInt(2008, Trade_.thisYear) //
				.mapIntObj(year -> hkexFactBook //
						.queryLeadingCompaniesByMarketCap(year - 1) //
						.filter(delisted::contains) //
						.toList()) //
				.toList());
	}

	@Test
	public void testLeadingCompanies() {
		var companies = hkexFactBook.queryLeadingCompaniesByMarketCap(2016);
		System.out.println(companies.toList());
		assertTrue(companies.isAny(symbol -> String_.equals(symbol, "0005.HK")));
	}

	@Test
	public void testMainBoard() {
		var companies = hkexFactBook.queryMainBoardCompanies(2012);
		System.out.println(companies.toList());
		assertTrue(companies.isAny(symbol -> String_.equals(symbol, "0005.HK")));
	}

}
