package suite.math;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import suite.Suite;
import suite.node.Atom;

public class SymbolicTest {

	private Symbolic sym = new Symbolic();

	@Test
	public void test() {
		Atom x = Atom.of("x");
		assertEquals("0", sym.d(x, Suite.parse("1")).toString());
		assertEquals("1", sym.d(x, Suite.parse("x")).toString());
		assertEquals("4 * x * x * x", sym.d(x, Suite.parse("x * x * x * x")).toString());
		assertEquals("neg 2 * inv x * inv x", sym.d(x, Suite.parse("2 / x")).toString());

		assertTrue(sym.fun(Suite.parse("x * x"), Atom.of("x")).apply(2f) == 4f);
	}

}
