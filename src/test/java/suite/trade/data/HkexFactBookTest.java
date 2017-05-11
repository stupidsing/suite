package suite.trade.data;

import static org.junit.Assert.assertTrue;

import org.junit.Test;

import suite.streamlet.Streamlet;
import suite.util.Util;

public class HkexFactBookTest {

	private HkexFactBook hkexFactBook = new HkexFactBook();

	@Test
	public void test() {
		Streamlet<String> companies = hkexFactBook.queryLeadingCompaniesByMarketCap(2012);
		System.out.println(companies.toList());
		assertTrue(companies.isAny(symbol -> Util.stringEquals(symbol, "0005.HK")));
	}

}
