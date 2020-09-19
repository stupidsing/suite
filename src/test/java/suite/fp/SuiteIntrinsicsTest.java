package suite.fp;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import suite.Suite;
import suite.node.Data;

public class SuiteIntrinsicsTest {

	@Test
	public void testMatch() {
		var fp0 = """
				use SUITE ~ chars:"1 + 2"
				| suite-parse
				| suite-match_{chars:".0 + .1"}
				| (`$n0; _;` => n0)
				| suite-to-chars
				| cs-to-string
				""";
		assertEquals("49;", Suite.evaluateFun(fp0, true).toString());
	}

	@Test
	public void testSubstitute() {
		var fp0 = """
				use SUITE ~ chars:"1 + 2"
				| suite-parse
				| (n => suite-substitute_{chars:"fn_{.0}"}_{n;})
				| suite-to-chars
				""";
		var n = Suite.evaluateFun(fp0, true);
		assertEquals("fn_{1 + 2}", Data.get(n).toString());
	}

}
