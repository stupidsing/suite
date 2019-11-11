package suite.math.sym;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import suite.Suite;
import suite.node.Atom;
import suite.node.Node;
import suite.node.util.Rewrite;

public class SymbolicTest {

	private Atom a = Atom.of("a");
	private Atom b = Atom.of("b");
	private Atom c = Atom.of("c");
	private Atom d = Atom.of("d");
	private Atom x = Atom.of("x");
	private Atom y = Atom.of("y");
	private Rewrite rw = new Rewrite();
	private Symbolic sym = new Symbolic();

	@Test
	public void test0() {
		verifyEquals("x + y", sym.polyize(Suite.parse("x + y"), y, x).g());
		verifyEquals("x * x + 2 * x + 1", sym.polyize(Suite.parse("(x + 1) ^ 2"), y, x).g());
		verifyEquals("x * x + neg 2 * x + 1", sym.polyize_xyn(Suite.parse("(x + neg 1) ^ 2")).g());
		verifyEquals("x * x * x + 3 * x * x + 3 * x + 1", sym.polyize(Suite.parse("(x + 1) ^ 3"), y, x).g());

		verifyEquals("" //
				+ "(a * a * a) * x * x * x" //
				+ " + ((3 * a * a) * b) * x * x" //
				+ " + ((3 * a) * b * b) * x" //
				+ " + b * b * b", sym.polyize(Suite.parse("(a * x + b) ^ 3"), a, b, x).g());
	}

	@Test
	public void test1() {
		verifySimplify("(x + 1) ^ 2", "x * x + 2 * x + 1", y, x, d, c, b, a);
		verifySimplify("(x + 1) ^ 3", "x * x * x + 3 * x * x + 3 * x + 1", y, x, d, c, b, a);
	}

	@Test
	public void testApply() {
		assertTrue(sym.fun(Suite.parse("x * x"), Atom.of("x")).apply(2f) == 4f);
	}

	@Test
	public void testCubic() {
		verifySimplify("(a * x + b) ^ 3", "" //
				+ "(a * a * a) * x * x * x" //
				+ " + ((3 * a * a) * b) * x * x" //
				+ " + ((3 * a) * b * b) * x" //
				+ " + b * b * b");

		verifySimplify("(a * x + neg b) ^ 3", "" //
				+ "(a * a * a) * x * x * x" //
				+ " + ((neg 3 * a * a) * b) * x * x" //
				+ " + ((3 * a) * b * b) * x" //
				+ " + neg 1 * b * b * b");

		var poly = rw.replace(x, //
				Suite.parse("y + neg (b * inv (3 * a))"), //
				Suite.parse("a * x * x * x + b * x * x + c * x + d"));

		verifySimplify(poly, "" //
				+ "a * y * y * y" //
				+ " + (c + (neg inv 3 * inv a) * b * b) * y" //
				+ " + d + ((neg inv 3 * inv a) * b) * c + ((2 * inv 27) * inv (a * a)) * b * b * b");
	}

	@Test
	public void testDifferentiation() {
		verifyEquals("0", sym.d(Suite.parse("1"), x));
		verifyEquals("1", sym.d(Suite.parse("x"), x));
		verifyEquals("4 * x * x * x", sym.d(Suite.parse("x * x * x * x"), x));
		verifyEquals("neg 2 * inv (x * x)", sym.d(Suite.parse("2 / x"), x));
	}

	@Test
	public void testDifferentiationSigmoid() {
		// actual: neg (neg 1 * exp neg x) * inv (1 + exp neg x) * inv (1 + exp neg x)
		// expect: (exp neg x) * inv (1 + exp neg x) * inv (1 + exp neg x)
		System.out.println(sym.d(Suite.parse("inv (1 + exp neg x)"), x));
	}

	@Test
	public void testIntegration() {
		verifyEquals("cos x + x * sin x", sym.i(Suite.parse("x * cos x"), x));
	}

	@Test
	public void testRational() {
		var f = Fractional.ofIntegral();
		assertEquals("7:6", f.fractionalize(Suite.parse("inv 3 + 5 * inv 6")).toString());
		assertEquals("1:4", f.fractionalize(Suite.parse("inv (6 * 4 * inv 6)")).toString());
	}

	@Test
	public void testSimplify() {
		verifySimplify("4", "4");
		verifySimplify("(x + 1) ^ 1", "x + 1", x);
		verifySimplify("(x + 1) ^ 2", "x * x + 2 * x + 1", x);
		verifySimplify("(x + b) ^ 2", "x * x + (2 * b) * x + b * b", x, b);
	}

	private void verifySimplify(String poly, String expected, Node... xs) {
		verifyEquals(expected, sym.simplify(Suite.parse(poly), xs));
	}

	private void verifySimplify(String poly, String expected) {
		verifyEquals(expected, sym.simplify(Suite.parse(poly), y, x, d, c, b, a));
	}

	private void verifySimplify(Node poly, String expected) {
		verifyEquals(expected, sym.simplify(poly, y, x, d, c, b, a));
	}

	private void verifyEquals(String expected, Node node) {
		var actual = node.toString();
		System.out.println(actual);
		assertEquals(expected, actual);
	}

}
