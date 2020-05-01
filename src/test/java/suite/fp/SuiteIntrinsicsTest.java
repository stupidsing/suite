package suite.fp;

import org.junit.jupiter.api.Test;
import suite.Suite;
import suite.node.Data;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class SuiteIntrinsicsTest {

	@Test
	public void testMatch() {
		var fp0 = "" //
				+ "use SUITE ~ chars:\"1 + 2\" \n" //
				+ "| suite-parse \n" //
				+ "| suite-match_{chars:\".0 + .1\"} \n" //
				+ "| (`$n0; _;` => n0) \n" //
				+ "| suite-to-chars \n" //
				+ "| cs-to-string";
		assertEquals("49;", Suite.evaluateFun(fp0, true).toString());
	}

	@Test
	public void testSubstitute() {
		var fp0 = "" //
				+ "use SUITE ~ chars:\"1 + 2\" \n" //
				+ "| suite-parse \n" //
				+ "| (n => suite-substitute_{chars:\"fn_{.0}\"}_{n;}) \n" //
				+ "| suite-to-chars";
		var n = Suite.evaluateFun(fp0, true);
		assertEquals("fn_{1 + 2}", Data.get(n).toString());
	}

}
