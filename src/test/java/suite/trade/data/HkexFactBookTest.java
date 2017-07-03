package suite.trade.data;

import static org.junit.Assert.assertTrue;

import java.util.HashSet;

import org.junit.Test;

import suite.primitive.streamlet.IntStreamlet;
import suite.streamlet.Streamlet;
import suite.trade.Trade_;
import suite.util.String_;

public class HkexFactBookTest {

	private HkexFactBook hkexFactBook = new HkexFactBook();

	@Test
	public void testDelist() {
		HashSet<String> delisted = new HashSet<>(hkexFactBook.queryDelisted().toList());

		System.out.println(delisted);
		System.out.println(IntStreamlet //
				.range(2008, Trade_.thisYear) //
				.mapIntObj(year -> hkexFactBook //
						.queryLeadingCompaniesByMarketCap(year - 1) //
						.filter(delisted::contains) //
						.toList()) //
				.toList());
	}

	@Test
	public void testLeadingCompanies() {
		Streamlet<String> companies = hkexFactBook.queryLeadingCompaniesByMarketCap(2017);
		System.out.println(companies.toList());
		assertTrue(companies.isAny(symbol -> String_.equals(symbol, "0005.HK")));
	}

	@Test
	public void testMainBoard() {
		Streamlet<String> companies = hkexFactBook.queryMainBoardCompanies(2012);
		System.out.println(companies.toList());
		assertTrue(companies.isAny(symbol -> String_.equals(symbol, "0005.HK")));
	}

}
