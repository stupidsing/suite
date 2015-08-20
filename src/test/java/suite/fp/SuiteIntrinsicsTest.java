package suite.fp;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import suite.Suite;

public class SuiteIntrinsicsTest {

	@Test
	public void test() {
		String fp = "using SUITE >> \"1 + 2\" \n" //
				+ "| cs-from-string \n" //
				+ "| suite-parse \n" //
				+ "| suite-match {chars:\".0 + .1\"} \n" //
				+ "| get {0} \n" //
				+ "| suite-to-chars \n" //
				+ "| cs-to-string";
		assertEquals("49;", Suite.evaluateFun(fp, true).toString());
	}

}
