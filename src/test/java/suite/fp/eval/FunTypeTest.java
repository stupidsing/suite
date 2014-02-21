package suite.fp.eval;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import suite.Suite;
import suite.lp.Journal;
import suite.lp.doer.Binder;
import suite.lp.doer.Generalizer;
import suite.node.Node;

public class FunTypeTest {

	@Test
	public void testBasic() {
		assertType("boolean", "4 = 8");
	}

	@Test
	public void testClass() {
		assertType("clazz", "" //
				+ "data clazz as EMPTY >> \n" //
				+ "define add = (clazz -> clazz) of (a => a) >> \n" //
				+ "add | {EMPTY}");

		assertType("boolean", "" //
				+ "data t as NIL >> \n" //
				+ "data t as BTREE (t, t) >> \n" //
				+ "let u = t of NIL >> \n" //
				+ "let v = t of NIL >> \n" //
				+ "v = BTREE (BTREE (NIL, NIL), NIL)");
	}

	@Test
	public void testClassOfClass() {
		assertType("c2 {boolean}", "" //
				+ "data (c0 {:t}) over :t as A :t >> \n" //
				+ "data (c1 {:t}) over :t as (c0 {:t}) >> \n" //
				+ "data (c2 {:t}) over :t as (c1 {:t}) >> \n" //
				+ "(c2 {boolean}) of (A true)");
	}

	@Test
	public void testDefineType() {
		getType("data weight as KG number >> \n" //
				+ "let v = weight of (KG 1) >> \n" //
				+ "v = KG 99");
		getType("repeat {23}");
	}

	@Test
	public void testFail() {
		String cases[] = { "1 + \"abc\"" //
				, "2 = true" //
				, "(f => f {0}) | 1" //
				, "define fib = (i2 => dummy => 1; fib {i2}) >> ()" //
		};

		// There is a problem in deriving type of 1:(fib {i2})...
		// Rule specified that right hand side of CONS should be a list,
		// however fib {i2} is a closure.
		for (String c : cases)
			getTypeMustFail(c);
	}

	@Test
	public void testFun() {
		assertType("number -> number", "a => a + 1");
		assertType("number", "define f = (a => a + 1) >> f {3}");
		assertType("boolean -> boolean -> boolean", "and");
		assertType("number -> [number]", "v => v; reverse {1;}");
	}

	@Test
	public void testGeneric() {
		assertType("[rb-tree {number}]", "" //
				+ "data (rb-tree {:t}) over :t as EMPTY >> \n" //
				+ "define map = (:a => :b => (:a -> :b) -> [:a] -> [:b]) of error >> \n" //
				+ "define add = ($t => $t -> rb-tree {$t}) of (v => EMPTY) >> \n" //
				+ "1; | map {add} \n" //
		);
	}

	@Test
	public void testInstance() {
		String define = "" //
				+ "data (list {:t}) over :t as NIL >> \n" //
				+ "data (list {:t}) over :t as NODE (:t, list {:t}) >> \n" //
				+ "data (list {:t}) over :t as NODE2 (:t, :t, list {:t}) >> \n" //
		;

		getType(define + "NIL");
		getType(define + "NODE (false, NIL)");
		getType(define + "NODE2 (1, 2, NODE (3, NIL))");

		assertType("boolean", define //
				+ "let n = NODE (true, NIL) >> NODE (false, n) = NIL");

		getTypeMustFail(define + "NODE");
		getTypeMustFail(define + "NODE 1");
		getTypeMustFail(define + "NODE (1, NODE (true, NIL))");
		getTypeMustFail(define + "NODE2 (1, true, NIL)");
		getTypeMustFail(define + "NODE2 (1, 2, NODE (true, NIL))");
		getTypeMustFail(define + "NODE (1, NIL) = NODE (false, NIL)");
		getTypeMustFail(define + "let n = NODE (true, NIL) >> NODE (1, n)");
	}

	@Test
	public void testList() {
		assertType("[number]", "1;");
		assertType("[[number]]", "\"a\"; \"b\"; \"c\"; \"d\";");
	}

	@Test
	public void testRbTree() {
		String fps = "using RB-TREE >> 0 until 10 | map {dict-add/ {1}} | apply | {EMPTY}";
		assertType("rb-tree {number, number}", fps);
	}

	@Test
	public void testStandard() {
		checkType("using STANDARD >> ends-with" //
				, "[T] -> _" //
				, "[T] -> [T] -> boolean");
		checkType("using STANDARD >> join" //
				, "T -> _" //
				, "T -> [[T]] -> [T]");
		checkType("using STANDARD >> merge" //
				, "([T] -> _) -> _" //
				, "([T] -> [T] -> [T]) -> [T] -> [T]");
	}

	@Test
	public void testTuple() {
		final String variant = "" //
				+ "data t as A >> \n" //
				+ "data t as B number >> \n" //
				+ "data t as C boolean >> \n";

		getType(variant + "A");
		getType(variant + "B 4");
		getType(variant + "C true");
		getType(variant + "if true then A else-if true then (B 3) else (C false)");
		getType("data btree as BTREE (number, number) >> BTREE (2, 3) = BTREE (4, 6)");

		getTypeMustFail(variant + "A 4");
		getTypeMustFail(variant + "B");
		getTypeMustFail(variant + "C 0");
		getTypeMustFail("data t1 as T1 (number, number) >> \n" //
				+ "data t2 as T2 (number, number) >> \n" //
				+ "T1 (2, 3) = T2 (2, 3)");
		getTypeMustFail("data btree as BTREE (number, number) >> \n" //
				+ "BTREE (2, 3) = BTREE (\"a\", 6)");
	}

	private void checkType(String fps, String bindTo, String ts) {
		Generalizer generalizer = new Generalizer();
		Journal journal = new Journal();
		Node type = getType(fps);

		assertTrue(Binder.bind(type, generalizer.generalize(Suite.parse(bindTo)), journal));
		assertEquals(Suite.parse(ts), type);
	}

	private void assertType(String type, String fp) {
		assertEquals(Suite.parse(type), getType(fp));
	}

	private static void getTypeMustFail(String fps) {
		try {
			getType(fps);
		} catch (RuntimeException ex) {
			return;
		}
		throw new RuntimeException("Cannot catch type error of: " + fps);
	}

	private static Node getType(String fps) {
		return Suite.evaluateFunType(fps);
	}

}
