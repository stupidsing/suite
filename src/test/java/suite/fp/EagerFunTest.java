package suite.fp;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.junit.Test;

import suite.Suite;
import suite.node.Atom;
import suite.node.Int;
import suite.node.Node;
import suite.node.Tree;
import suite.util.Fail;

public class EagerFunTest {

	@Test
	public void testAppend() {
		assertEquals( //
				Suite.parse("1; 2; 3; 4; 5; 6; 7; 8;"), //
				eval("append_{1; 2; 3; 4;}_{5; 6; 7; 8;}"));
	}

	@Test
	public void testApply() {
		assertEquals(Int.of(2), eval("apply_{(a => 2);}_{1}"));
		assertEquals(Int.of(2), eval("apply_{`/ 5`; `* 2`; `+ 1`;}_{4}"));
	}

	@Test
	public void testBisect() {
		assertEquals( //
				eval("(0; 2; 4; 6; 8;), (1; 3; 5; 7; 9;)"), //
				eval("0; 1; 2; 3; 4; 5; 6; 7; 8; 9; | bisect_{`= 0` . `% 2`}"));
	}

	@Test
	public void testClosure() {
		assertEquals(Int.of(7), eval("" //
				+ "define add := `+` ~ add_{3}_{4}"));

		assertEquals(Int.of(20), eval("" //
				+ "define p := `+ 1` ~ \n" //
				+ "define q := n => p_{n} * 2 ~ \n" //
				+ "q_{9}"));
	}

	@Test
	public void testConcat() {
		assertEquals( //
				Suite.parse("1; 2; 3; 4; 5; 6;"), //
				eval("concat_{(1; 2;); (3; 4;); (5; 6;);}"));
	}

	@Test
	public void testContains() {
		assertEquals(Atom.TRUE, eval("contains_{8; 9;}_{7; 8; 9; 10; 11;}"));
		assertEquals(Atom.FALSE, eval("contains_{11; 12;}_{7; 8; 9; 10; 11;}"));
	}

	@Test
	public void testCross() {
		var fp0 = "" //
				+ "cross_{a => b => a; b;}_{7; 8; 9;}_{1; 2;}";
		assertEquals(Suite.parse("" //
				+ "((7; 1;); (7; 2;);); " //
				+ "((8; 1;); (8; 2;);); " //
				+ "((9; 1;); (9; 2;););") //
				, eval(fp0));

		var fp1 = "" //
				+ "data T as A ~ \n" //
				+ "data T as B ~ \n" //
				+ "data T as C ~ \n" //
				+ "let list1 := [T] of (A; B; C;) ~ \n" //
				+ "let result := ( \n" //
				+ "    (A, 1,; A, 2,;); \n" //
				+ "    (B, 1,; B, 2,;); \n" //
				+ "    (C, 1,; C, 2,;); \n" //
				+ ") ~ \n" //
				+ "cross_{a => b => (a, b,)}_{list1}_{1; 2;} = result";
		assertEquals(Atom.TRUE, eval(fp1));
	}

	@Test
	public void testEndsWith() {
		assertEquals(Atom.TRUE, eval("ends-with_{1; 2; 3;}_{4; 5; 6; 1; 2; 3;}"));
		assertEquals(Atom.FALSE, eval("ends-with_{1; 2; 3;}_{4; 5; 3; 1; 2; 6;}"));
	}

	@Test
	public void testEquals() {
		assertEquals(Atom.TRUE, eval("() = ()"));
		assertEquals(Atom.FALSE, eval("(1; 2;) = (1; 3;)"));
	}

	@Test
	public void testFibonacci() {
		assertEquals(Int.of(89), eval("" //
				+ "define fib := n => \n" //
				+ "    if (1 < n) \n" //
				+ "    then (fib_{n - 1} + fib_{n - 2}) \n" //
				+ "    else 1 \n" //
				+ "~ \n" //
				+ "fib_{10}"));
	}

	@Test
	public void testFilter() {
		assertEquals(Suite.parse("4; 6;"), eval("filter_{n => n % 2 = 0}_{3; 4; 5; 6;}"));
	}

	@Test
	public void testFlip() {
		assertEquals(Int.of(2), eval("flip_{`-`}_{3}_{5}"));
	}

	@Test
	public void testFold() {
		assertEquals(Int.of(324), eval("fold_{`*`}_{2; 3; 6; 9;}"));
		assertEquals(Int.of(79), eval("fold-left_{`-`}_{100}_{6; 7; 8;}"));
		assertEquals(Int.of(-93), eval("fold-right_{`-`}_{100}_{6; 7; 8;}"));
	}

	@Test
	public void testGcd() {
		var f = Suite.parse("gcd_{6}_{9}");
		var c = Suite.fcc(f);
		c.addLibrary("MATH");
		assertEquals(Int.of(3), Suite.evaluateFun(c));
	}

	@Test
	public void testGroup() {
		assertEquals(eval("1, (2; 5;); 2, (1; 4;); 3, (0; 3;);"), eval("group_{3, 0; 2, 1; 1, 2; 3, 3; 2, 4; 1, 5;}"));
	}

	@Test
	public void testIf() {
		assertEquals(Int.of(0), eval("if (4 < 3) then 1 else 0"));
		assertEquals(Int.of(1), eval("if (3 = 3) then 1 else 0"));
		assertEquals(Int.of(1), eval("if (1 = 2) then 0 else-if (2 = 2) then 1 else 2"));
	}

	@Test
	public void testIfBind() {
		assertEquals(Int.of(1), eval("if-bind (1 := 1) then 1 else 0"));

		assertEquals(Int.of(1), eval("if-bind (1; 2; := 1; 2;) then 1 else 0"));
		assertEquals(Int.of(0), eval("if-bind (1; 2; := 2; 2;) then 1 else 0"));

		assertEquals(Int.of(1), eval("let v := 1; 2; ~ if-bind (v := 1; 2;) then 1 else 0"));
		assertEquals(Int.of(0), eval("let v := 1; 2; ~ if-bind (v := 1; 3;) then 1 else 0"));

		assertEquals(Int.of(0), eval("let v := true, 1, 2, ~ if-bind (v := true, $i, 3,) then i else 0"));
		assertEquals(Int.of(1), eval("let v := true, 1, 2, ~ if-bind (v := true, $i, 2,) then i else 0"));
		assertEquals(Int.of(1), eval("if-bind (1, 2, := $i, 2,) then i else 0"));

		assertEquals(Int.of(3), eval("" //
				+ "data T as A ~ \n" //
				+ "data T as B number ~ \n" //
				+ "data T as C boolean ~ \n" //
				+ "let e := B 3 ~ \n" //
				+ "if-bind (e := B $i) then i else 0"));

		assertEquals(Int.of(0), eval("" //
				+ "data T as A ~ \n" //
				+ "data T as B number ~ \n" //
				+ "data T as C boolean ~ \n" //
				+ "let e := B 3 ~ \n" //
				+ "let f := C false ~ \n" //
				+ "if-bind (e := f) then 1 else 0"));
	}

	// after replacing call stack with activation chain, this test would not
	// stack overflow but exhaust all memory. Test case will not be executed.
	// @Test
	public void testInfiniteLoop() {
		try {
			// this would fail stack over during type check, so skip that
			Suite.evaluateFun("skip-type-check (e => e_{e}) {e => e_{e}}", false);
			Fail.t();
		} catch (Throwable th) {
		}
	}

	@Test
	public void testJoin() {
		assertEquals(Int.of(19), eval("" //
				+ "define p := `* 2` ~ \n" //
				+ "define q := `+ 1` ~ \n" //
				+ "define r := q . p ~ \n" //
				+ "r_{9}"));

		assertEquals(Int.of(13), eval("" //
				+ "define p := `+ 1` ~ \n" //
				+ "define q := `* 2` ~ \n" //
				+ "define r := `- 3` ~ \n" //
				+ "(p . q . r)_{9}"));

		assertEquals(Int.of(17), eval("" //
				+ "define p := `+ 1` ~ \n" //
				+ "define q := `* 2` ~ \n" //
				+ "define r := `- 3` ~ \n" //
				+ "9 | p | q | r"));
	}

	@Test
	public void testLength() {
		assertEquals(Int.of(5), eval("length_{3; 3; 3; 3; 3;}"));
	}

	@Test
	public void testLog() {
		assertEquals(Int.of(8), eval("log_{4 + 4}"));
		assertEquals(Int.of(1), eval("if (1 = 1) then 1 else (1 / 0)"));
		assertEquals(Int.of(1), eval("if true then 1 else (log2_{\"shouldn't appear\"}_{1 / 0})"));
		assertEquals(Int.of(1), eval("if false then 1 else (log2_{\"should appear\"}_{1})"));
	}

	@Test
	public void testMap() {
		assertEquals(Suite.parse("5; 6; 7;"), eval("map_{`+ 2`}_{3; 4; 5;}"));
	}

	@Test
	public void testMergeSort() {
		assertEquals( //
				Suite.parse("0; 1; 2; 3; 4; 5; 6; 7; 8; 9;"), //
				eval("merge-sort_{5; 3; 2; 8; 6; 4; 1; 0; 9; 7;}"));
	}

	@Test
	public void testOperator() {
		assertEquals(Atom.TRUE, eval("" //
				+ "and_{1 = 1}_{or_{1 = 0}_{1 = 1}}"));

		assertEquals(Atom.FALSE, eval("" //
				+ "data T as A ~ \n" //
				+ "data T as B ~ \n" //
				+ "let list1 := [T] of () ~ A = B"));
	}

	@Test
	public void testPartition() {
		assertEquals( //
				eval("(1; 3; 0; 2; 4;), (5; 7; 9; 6; 8;)"), //
				eval("partition_{`< 5`}_{1; 3; 5; 7; 9; 0; 2; 4; 6; 8;}"));
	}

	@Test
	public void testQuickSort() {
		assertEquals( //
				Suite.parse("0; 1; 2; 3; 4; 5; 6; 7; 8; 9;"), //
				eval("quick-sort_{`<`}_{5; 3; 2; 8; 6; 4; 1; 0; 9; 7;}"));
	}

	@Test
	public void testRange() {
		assertEquals( //
				Suite.parse("2; 5; 8; 11;"), //
				eval("range_{2}_{14}_{3}"));
	}

	@Test
	public void testReplace() {
		assertEquals( //
				eval("\"abcghighijklmnopqrstuvwxyzabcghighi\""), //
				eval("replace_{\"def\"}_{\"ghi\"}_{\"abcdefghijklmnopqrstuvwxyzabcdefghi\"}"));
	}

	@Test
	public void testReplicate() {
		assertEquals(Suite.parse("3; 3; 3; 3;"), eval("replicate_{4}_{3}"));
	}

	@Test
	public void testReverse() {
		assertEquals(Suite.parse("5; 4; 3; 2; 1;"), eval("reverse_{1; 2; 3; 4; 5;}"));
	}

	@Test
	public void testStartsWith() {
		assertEquals(Atom.TRUE, eval("starts-with_{1; 2; 3;}_{1; 2; 3; 4; 5; 6;}"));
		assertEquals(Atom.FALSE, eval("starts-with_{1; 2; 3;}_{1; 2; 4; 3; 5; 6;}"));
	}

	@Test
	public void testSubstring() {
		assertEquals(eval("\"abcdefghij\""), eval("substring_{0}_{10}_{\"abcdefghij\"}"));
		assertEquals(eval("\"ef\""), eval("substring_{4}_{6}_{\"abcdefghij\"}"));
		assertEquals(eval("\"cdefgh\""), eval("substring_{2}_{-2}_{\"abcdefghij\"}"));
	}

	@Test
	public void testSwitch() {
		assertEquals(eval("\"B\""), eval("" //
				+ "define switch := \n" //
				+ "    case \n" //
				+ "    || 1 => \"A\" \n" //
				+ "    || 2 => \"B\" \n" //
				+ "    || 3 => \"C\" \n" //
				+ "    || anything => \"D\" \n" //
				+ "~ \n" //
				+ "switch_{2}"));
	}

	@Test
	public void testSys() {
		assertNotNull(Tree.decompose(eval("cons_{1}_{2;}")));
		assertEquals(Int.of(1), eval("head_{1; 2; 3;}"));
		assertNotNull(Tree.decompose(eval("tail_{1; 2; 3;}")));
	}

	// lazy programs are prone to stack overflow; even a summing program for a
	// long list of numbers produces long enough thunks/de-thunks to blow up.
	// eager programs with tail code optimization are more resistant.
	@Test
	public void testTailRecursion() {
		assertEquals(Int.of(65536) //
				, eval("10 | replicate_{65536} | reverse | length"));

		assertEquals(Int.of((1 + 16384) * 16384 / 2) //
				, eval("define sum := n => s => if (0 < n) then (sum_{n - 1}_{s + n}) else s ~ sum_{16384}_{0}"));
	}

	@Test
	public void testTails() {
		assertEquals( //
				Suite.parse("(1; 2; 3;); (2; 3;); (3;); ();"), //
				eval("tails_{1; 2; 3;}"));
	}

	@Test
	public void testTake() {
		assertEquals( //
				Suite.parse("1; 2; 3; 4;"), //
				eval("take_{4}_{1; 2; 3; 4; 5; 6; 7;}"));
	}

	@Test
	public void testUniq() {
		assertEquals( //
				Suite.parse("1; 2; 3; 5; 2;"), //
				eval("uniq_{1; 2; 2; 2; 3; 5; 2;}"));
	}

	@Test
	public void testZip() {
		assertEquals(Suite.parse("(1; 5;); (2; 6;); (3; 7;);"), eval("" //
				+ "define zip-up := zip_{a => b => a; b;} ~ \n" //
				+ "zip-up_{1; 2; 3;}_{5; 6; 7;}"));
	}

	private static Node eval(String f) {
		return Suite.evaluateFun(f, false);
	}

}
