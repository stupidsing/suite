package suite.trade;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.junit.Test;

import suite.streamlet.Read;
import suite.util.Util;

public class HkexTest {

	private Hkex hkex = new Hkex();

	@Test
	public void testList() {
		List<Asset> companies = hkex.queryCompanies().toList();
		System.out.println(companies);

		for (Asset company : companies)
			System.out.println("+ \"\\n" + company.code + "|" + company.name + "|" + company.marketCap + "\" //");

		String name = Read.from(companies) //
				.filter(fixie -> Util.stringEquals(fixie.code, "0005")) //
				.uniqueResult().name;

		assertTrue(name.equals("HSBC Holdings plc"));
	}

	@Test
	public void testQueryBoardLot() {
		assertEquals(400, hkex.queryBoardLot("0005.HK"));
		assertEquals(100, hkex.queryBoardLot("0700.HK"));
	}

}
