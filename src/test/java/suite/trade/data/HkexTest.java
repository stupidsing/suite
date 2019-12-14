package suite.trade.data;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import primal.MoreVerbs.Read;
import primal.Verbs.Equals;

public class HkexTest {

	private Hkex hkex = new Hkex();

	@Test
	public void testList() {
		var companies = hkex.queryCompanies().toList();
		System.out.println(companies);

		for (var company : companies)
			System.out.println("+ \"\\n" + company.symbol //
					+ "|" + company.name //
					+ "|" + company.lotSize //
					+ "|" + company.marketCap //
					+ "\" //");

		var name = Read //
				.from(companies) //
				.filter(fixie -> Equals.string(fixie.symbol, "0005.HK")) //
				.uniqueResult().name;

		assertTrue(name.equals("HSBC Holdings plc"));
	}

	@Test
	public void testQueryCompany() {
		assertEquals("HSBC Holdings plc", hkex.queryCompany("0005.HK").name);
	}

	@Test
	public void testQueryHangSengIndex() {
		assertTrue(20000f < hkex.queryHangSengIndex());
	}

}
