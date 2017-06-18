package suite.trade.data;

import static org.junit.Assert.assertTrue;

import java.util.List;

import org.junit.Test;

import suite.streamlet.Read;
import suite.streamlet.Streamlet;
import suite.util.String_;

public class HkexFactBookTest {

	private HkexFactBook hkexFactBook = new HkexFactBook();

	@Test
	public void test() {
		Streamlet<String> companies = hkexFactBook.queryLeadingCompaniesByMarketCap(2012);
		System.out.println(companies.toList());
		assertTrue(companies.isAny(symbol -> String_.equals(symbol, "0005.HK")));
	}

	@Test
	public void testMainBoard() {
		List<String> companies = hkexFactBook.queryMainBoardCompanies(2012);
		System.out.println(companies);
		assertTrue(Read.from(companies).isAny(symbol -> String_.equals(symbol, "0005.HK")));
	}

}
