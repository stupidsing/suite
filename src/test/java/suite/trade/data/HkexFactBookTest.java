package suite.trade.data;

import static org.junit.Assert.assertTrue;

import org.junit.Test;

import suite.streamlet.Streamlet;
import suite.trade.Asset;
import suite.util.Util;

public class HkexFactBookTest {

	private HkexFactBook hkexFactBook = new HkexFactBook();

	@Test
	public void test() {
		Streamlet<Asset> companies = hkexFactBook.queryLeadingCompaniesByMarketCap(2012);
		System.out.println(companies.toList());
		assertTrue(companies.isAny(asset -> Util.stringEquals(asset.code, "0005.HK")));
	}

}
