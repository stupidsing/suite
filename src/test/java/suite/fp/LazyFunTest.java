package suite.fp;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import suite.Suite;
import suite.node.Atom;
import suite.node.Int;
import suite.node.Node;

public class LazyFunTest {

	@Test
	public void testChunk() {
		assertEquals( //
				Suite.parse("(0; 1; 2;); (3; 4; 5;); (6; 7; 8;); (9; 10; 11;); (12; 13; 14;); (15;);"), //
				eval("range_{0}_{16}_{1} | chunk_{3}"));
	}

	@Test
	public void testClosure() {
		assertEquals(Suite.parse("4"), eval("define v := number of 4 ~ (i => j => v)_{1}_{2}"));
	}

	@Test
	public void testCorecursion() {
		assertEquals(Atom.TRUE, eval("" //
				+ "define seq := n => n; seq_{n} ~ \n" //
				+ "head_{seq_{0}} = 0"));

		assertEquals(Int.of(89), eval("" // real co-recursion!
				+ "define fib := i1 => i2 => i2; fib_{i2}_{i1 + i2} ~ \n" //
				+ "fib_{0}_{1} | get_{10}"));
	}

	@Test
	public void testDefines() {
		assertEquals(Int.of(62), eval("" //
				+ "lets ( \n" //
				+ "    a := n => if (0 < n) then (b_{n - 1} * 2) else 0 # \n" //
				+ "    b := n => if (0 < n) then (a_{n - 1} + 1) else 0 # \n" //
				+ ") ~ a_{10}"));
	}

	@Test
	public void testFibonacci() {
		assertEquals(Int.of(89), eval("" //
				+ "define fib := \n" //
				+ "    1; 1; zip_{`+`}_{fib}_{tail_{fib}} \n" //
				+ "~ fib | get_{10}"));

		assertEquals(Int.of(144), eval("" //
				+ "define fib := x => \n" //
				+ "    if (x = `$a; $y`) then \n" //
				+ "        if (y = `$b; $z`) then \n" //
				+ "            (fib_{y} + fib_{z}) \n" //
				+ "        else 1 \n" //
				+ "    else 0 \n" //
				+ "~ fib_{0; 0; 0; 0; 0; 0; 0; 0; 0; 0; 0; 0; }"));
	}

	@Test
	public void testFix() {
		assertEquals(Suite.parse("0; 0; 0;"), eval("fix_{cons_{0}} | take_{3}"));
	}

	@Test
	public void testFold() {
		assertEquals(Suite.parse("0; 1; 2; 3; 4;"), eval("" //
				+ "define inf-series := n => n; inf-series_{n + 1} ~ " //
				+ "0 | inf-series | fold-right_{`;`}_{} | take_{5}"));

		// on the other hand, same call using fold-left would result in infinite
		// loop, like this:
		// define is = (n => n; is_{n + 1}) ~
		// 0 | is | fold-left_{`;`/}_{} | take_{5}
	}

	@Test
	public void testIterate() {
		assertEquals(Int.of(65536), eval("iterate_{`* 2`}_{1} | get_{16}"));
	}

	@Test
	public void testLines() {
		assertEquals( //
				Suite.parse("(0; 1; 2; 3; 4; 5; 10;); (6; 7; 8; 9; 10;); (2; 3; 4;);"), //
				eval("lines_{0; 1; 2; 3; 4; 5; 10; 6; 7; 8; 9; 10; 2; 3; 4;}"));
	}

	@Test
	public void testString() {
		assertEquals(Int.of(-34253924), eval("str-to-int_{\"-34253924\"}"));
		assertEquals(Atom.TRUE, eval("\"-34253924\" = int-to-str_{-34253924}"));
	}

	@Test
	public void testSubstitution() {
		assertEquals(Int.of(8), eval("define v := 4 ~ v + v"));
	}

	@Test
	public void testSystem() {
		assertEquals(Atom.TRUE, eval("1 = 1"));
		assertEquals(Atom.FALSE, eval("1 = 2"));
		eval("cons_{1}_{}");
		eval("head_{1; 2; 3;}");
		eval("tail_{1; 2; 3;}");
	}

	@Test
	public void testTakeWhile() {
		assertEquals( //
				Suite.parse("0; 1; 2; 3;"), //
				eval("take-while_{`<= 3`}_{0; 1; 2; 3; 4; 5; 6; 7; 8; 9; }"));

		assertEquals( //
				Suite.parse("0; 1; 2; 3;"), //
				eval("0 | iterate_{`+ 1`} | take-while_{`<= 3`}"));
	}

	@Test
	public void testTranspose() {
		assertEquals( //
				Suite.parse("(1; 4; 7;); (2; 5; 8;); (3; 6; 9;);"), //
				eval("transpose_{(1; 2; 3;); (4; 5; 6;); (7; 8; 9;);}"));
	}

	private static Node eval(String f) {
		return Suite.evaluateFun(f, true);
	}

}
