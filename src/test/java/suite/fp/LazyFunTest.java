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
				eval("range {0} {16} {1} | chunk {3}"));
	}

	@Test
	public void testClosure() {
		assertEquals(Suite.parse("4"), eval("define v := number of 4 ~ (i => j => v) {1} {2}"));
	}

	@Test
	public void testCorecursion() {
		assertEquals(Atom.TRUE, eval("" //
				+ "define seq := n => n; seq {n} ~ \n" //
				+ "head {seq {0}} = 0"));

		assertEquals(Int.of(89), eval("" // real co-recursion!
				+ "define fib := i1 => i2 => i2; fib {i2} {i1 + i2} ~ \n" //
				+ "fib {0} {1} | get {10}"));
	}

	@Test
	public void testDefines() {
		assertEquals(Int.of(62), eval("" //
				+ "lets ( \n" //
				+ "    a := n => if (0 < n) then (b {n - 1} * 2) else 0 # \n" //
				+ "    b := n => if (0 < n) then (a {n - 1} + 1) else 0 # \n" //
				+ ") ~ a {10}"));
	}

	@Test
	public void testFibonacci() {
		assertEquals(Int.of(89), eval("" //
				+ "define fib := \n" //
				+ "    1; 1; zip {`+`} {fib} {tail {fib}} \n" //
				+ "~ fib | get {10}"));

		assertEquals(Int.of(144), eval("" //
				+ "define fib := x => \n" //
				+ "    if (x = `$a; $y`) then \n" //
				+ "        if (y = `$b; $z`) then \n" //
				+ "            (fib {y} + fib {z}) \n" //
				+ "        else 1 \n" //
				+ "    else 0 \n" //
				+ "~ fib {0; 0; 0; 0; 0; 0; 0; 0; 0; 0; 0; 0; }"));
	}

	@Test
	public void testFix() {
		assertEquals(Suite.parse("0; 0; 0;"), eval("fix {cons {0}} | take {3}"));
	}

	@Test
	public void testFold() {
		assertEquals(Suite.parse("0; 1; 2; 3; 4;"), eval("" //
				+ "define inf-series := n => n; inf-series {n + 1} ~ " //
				+ "0 | inf-series | fold-right {`;`} {} | take {5}"));

		// on the other hand, same call using fold-left would result in infinite
		// loop, like this:
		// define is = (n => n; is {n + 1}) ~
		// 0 | is | fold-left {`;`/} {} | take {5}
	}

	@Test
	public void testIterate() {
		assertEquals(Int.of(65536), eval("iterate {`* 2`} {1} | get {16}"));
	}

	@Test
	public void testLines() {
		assertEquals( //
				Suite.parse("(0; 1; 2; 3; 4; 5; 10;); (6; 7; 8; 9; 10;); (2; 3; 4;);"), //
				eval("lines {0; 1; 2; 3; 4; 5; 10; 6; 7; 8; 9; 10; 2; 3; 4;}"));
	}

	@Test
	public void testString() {
		assertEquals(Int.of(-34253924), eval("str-to-int {\"-34253924\"}"));
		assertEquals(Atom.TRUE, eval("\"-34253924\" = int-to-str {-34253924}"));
	}

	@Test
	public void testSubstitution() {
		assertEquals(Int.of(8), eval("define v := 4 ~ v + v"));
	}

	@Test
	public void testSystem() {
		assertEquals(Atom.TRUE, eval("1 = 1"));
		assertEquals(Atom.FALSE, eval("1 = 2"));
		eval("cons {1} {}");
		eval("head {1; 2; 3;}");
		eval("tail {1; 2; 3;}");
	}

	@Test
	public void testTakeWhile() {
		assertEquals( //
				Suite.parse("0; 1; 2; 3;"), //
				eval("take-while {`<= 3`} {0; 1; 2; 3; 4; 5; 6; 7; 8; 9; }"));

		assertEquals( //
				Suite.parse("0; 1; 2; 3;"), //
				eval("0 | iterate {`+ 1`} | take-while {`<= 3`}"));
	}

	@Test
	public void testTranspose() {
		assertEquals( //
				Suite.parse("(1; 4; 7;); (2; 5; 8;); (3; 6; 9;);"), //
				eval("transpose {(1; 2; 3;); (4; 5; 6;); (7; 8; 9;);}"));
	}

	private static Node eval(String f) {
		return Suite.evaluateFun(f, true);
	}

}
