package suite.math;

import org.junit.Test;

import suite.Suite;
import suite.node.Atom;

public class SymbolicTest {

	private Symbolic sym = new Symbolic();

	@Test
	public void test() {
		Atom x = Atom.of("x");
		System.out.println(sym.d(x, Suite.parse("1")));
		System.out.println(sym.d(x, Suite.parse("x")));
		System.out.println(sym.d(x, Suite.parse("x * x")));
		System.out.println(sym.d(x, Suite.parse("2 / x")));
	}

}
