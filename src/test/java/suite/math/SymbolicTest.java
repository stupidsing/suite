package suite.math;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import suite.Suite;
import suite.node.Atom;
import suite.node.Node;
import suite.node.util.TreeRewriter;

public class SymbolicTest {

	private Atom x = Atom.of("x");
	private Atom y = Atom.of("y");
	private Symbolic sym = new Symbolic();
	private TreeRewriter trw = new TreeRewriter();

	@Test
	public void test() {
		assertEquals("0", sym.d(x, Suite.parse("1")).toString());
		assertEquals("1", sym.d(x, Suite.parse("x")).toString());
		assertEquals("4 * x * x * x", sym.d(x, Suite.parse("x * x * x * x")).toString());
		assertEquals("neg 2 * inv x * inv x", sym.d(x, Suite.parse("2 / x")).toString());

		assertTrue(sym.fun(Suite.parse("x * x"), Atom.of("x")).apply(2f) == 4f);
	}

	@Test
	public void testCubic() {
		System.out.println(sym.simplify(x, Suite.parse("(a * x + b) ^ 3")));
		Node poly = trw.replace(x, Suite.parse("y + neg (b * inv (3 * a))"), Suite.parse("a * x * x * x + b * x * x + c * x + d"));
		System.out.println(sym.simplify(y, poly));
	}

}
