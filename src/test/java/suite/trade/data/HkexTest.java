package suite.trade.data;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.junit.Test;

import suite.streamlet.Read;
import suite.trade.Asset;
import suite.util.Util;

public class HkexTest {

	private Hkex hkex = new Hkex();

	@Test
	public void testList() {
		List<Asset> companies = hkex.queryCompanies().toList();
		System.out.println(companies);

		for (Asset company : companies)
			System.out.println("+ \"\\n" + company.symbol + "|" + company.name + "|" + company.marketCap + "\" //");

		String name = Read.from(companies) //
				.filter(fixie -> Util.stringEquals(fixie.symbol, "0005.HK")) //
				.uniqueResult().name;

		assertTrue(name.equals("HSBC Holdings plc"));
	}

	@Test
	public void testQueryBoardLot() {
		assertEquals(400, hkex.queryBoardLot("0005.HK"));
		assertEquals(100, hkex.queryBoardLot("0700.HK"));
	}

	@Test
	public void testQueryCompany() {
		assertEquals("HSBC Holdings plc", hkex.queryCompany("0005.HK").name);
	}

	@Test
	public void testQueryHangSengIndex() {
		assertTrue(20000f < hkex.queryHangSengIndex());
	}

	@Test
	public void testQueryPreviousClose() {
		assertTrue(60f < hkex.queryPreviousClose("0005.HK"));
	}

}
