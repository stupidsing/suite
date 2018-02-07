package suite.math;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import suite.Suite;
import suite.node.Atom;
import suite.node.Node;
import suite.node.util.TreeRewriter;

public class SymbolicTest {

	private Atom a = Atom.of("a");
	private Atom b = Atom.of("b");
	private Atom x = Atom.of("x");
	private Atom y = Atom.of("y");
	private Symbolic sym = new Symbolic();
	private TreeRewriter trw = new TreeRewriter();

	@Test
	public void testApply() {
		assertTrue(sym.fun(Suite.parse("x * x"), Atom.of("x")).apply(2f) == 4f);
	}

	@Test
	public void testCubic() {
		verifyEquals("4", sym.simplify(Suite.parse("4")));
		System.out.println(sym.simplify(Suite.parse("(a * x + b) ^ 3"), x, a, b));
		Node poly = trw.replace(x, //
				Suite.parse("y + neg (b * inv (3 * a))"), //
				Suite.parse("a * x * x * x + b * x * x + c * x + d"));
		System.out.println(sym.simplify(poly, y, a, b));
	}

	@Test
	public void testDifferentiation() {
		verifyEquals("0", sym.d(Suite.parse("1"), x));
		verifyEquals("1", sym.d(Suite.parse("x"), x));
		verifyEquals("4 * x * x * x", sym.d(Suite.parse("x * x * x * x"), x));
		verifyEquals("neg 2 * inv (x * x)", sym.d(Suite.parse("2 / x"), x));
	}

	@Test
	public void testIntegration() {
		verifyEquals("cos x + x * sin x", sym.i(Suite.parse("x * cos x"), x));
	}

	private void verifyEquals(String expected, Node node) {
		assertEquals(expected, node.toString());
	}

}
