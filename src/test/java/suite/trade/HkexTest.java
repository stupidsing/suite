package suite.trade;

import static org.junit.Assert.assertTrue;

import java.util.List;

import org.junit.Test;

import suite.adt.Fixie;
import suite.adt.Fixie.D_;
import suite.streamlet.Read;
import suite.util.Util;

public class HkexTest {

	@Test
	public void test() {
		List<Fixie<String, String, Integer, D_, D_, D_, D_, D_, D_, D_>> fixies = new Hkex().list();
		System.out.println(fixies);
		String code = Read.from(fixies).filter(fixie -> Util.stringEquals(fixie.t0, "5")).uniqueResult().t1;
		assertTrue(code.equals("HSBC Holdings plc"));
	}

}
