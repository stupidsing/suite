package suite.trade.data;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static suite.util.Streamlet_.forInt;

import java.util.HashSet;

import org.junit.jupiter.api.Test;

import primal.Verbs.Equals;
import suite.trade.Trade_;

public class HkexFactBookTest {

	private HkexFactBook hkexFactBook = new HkexFactBook();

	@Test
	public void testDelist() {
		var delisted = new HashSet<String>(hkexFactBook.queryDelisted().toList());

		System.out.println(delisted);
		System.out.println(forInt(2008, Trade_.thisYear) //
				.mapIntObj(year -> hkexFactBook //
						.queryYearlyLeadingCompaniesByMarketCap(year - 1) //
						.filter(delisted::contains) //
						.toList()) //
				.toList());
	}

	@Test
	public void testLeadingCompanies() {
		var companies = hkexFactBook.queryYearlyLeadingCompaniesByMarketCap(2016);
		System.out.println(companies.toList());
		assertTrue(companies.isAny(symbol -> Equals.string(symbol, "0005.HK")));
	}

	@Test
	public void testLeadingCompaniesByQuarter() {
		var companies = hkexFactBook.queryQuarterlyLeadingCompaniesByMarketCap(2018, "3rd-Quarter");
		System.out.println(companies.toList());
		assertTrue(companies.isAny(symbol -> Equals.string(symbol, "0005.HK")));
	}

	@Test
	public void testMainBoard() {
		var companies = hkexFactBook.queryMainBoardCompanies(2019);
		System.out.println(companies.toList());
		assertTrue(companies.isAny(symbol -> Equals.string(symbol, "0005.HK")));
	}

}
