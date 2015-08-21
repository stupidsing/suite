package suite.fp;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import suite.Suite;
import suite.node.Data;
import suite.node.Node;

public class SuiteIntrinsicsTest {

	@Test
	public void testMatch() {
		String fp0 = "" //
				+ "using SUITE >> chars:\"1 + 2\" \n" //
				+ "| suite-parse \n" //
				+ "| suite-match {chars:\".0 + .1\"} \n" //
				+ "| get {0} \n" //
				+ "| suite-to-chars \n" //
				+ "| cs-to-string";
		assertEquals("49;", Suite.evaluateFun(fp0, true).toString());
	}

	@Test
	public void testSubstitute() {
		String fp0 = "" //
				+ "using SUITE >> chars:\"1 + 2\" \n" //
				+ "| suite-parse \n" //
				+ "| (n => suite-substitute {chars:\"fn {.0}\"} {n;}) \n" //
				+ "| suite-to-chars";
		Node n = Suite.evaluateFun(fp0, true);
		assertEquals("fn {1 + 2}", Data.get(n).toString());
	}

}
