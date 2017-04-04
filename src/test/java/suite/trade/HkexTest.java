package suite.trade;

import static org.junit.Assert.assertTrue;

import java.util.List;

import org.junit.Test;

import suite.streamlet.Read;
import suite.trade.Hkex.Company;
import suite.util.Util;

public class HkexTest {

	@Test
	public void test() {
		List<Company> companies = new Hkex().list();
		System.out.println(companies);

		for (Company company : companies)
			System.out.println("+ \"\\n" + company.code + "|" + company.name + "|" + company.marketCap + "\" //");

		String name = Read.from(companies) //
				.filter(fixie -> Util.stringEquals(fixie.code, "0005")) //
				.uniqueResult().name;

		assertTrue(name.equals("HSBC Holdings plc"));
	}

}
