package suite.fp; import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static suite.util.Friends.fail;

import org.junit.Test;

import suite.Suite;
import suite.lp.Trail;
import suite.lp.doer.Binder;
import suite.lp.sewing.impl.SewingGeneralizerImpl;
import suite.node.Node;
import suite.node.util.Comparer;

public class FunTypeTest {

	@Test
	public void testBasic() {
		assertType("boolean", "4 = 8");
		assertType("number -> number -> number", "`*` . `+ 1`");
	}

	@Test
	public void testBind() {
		var data = "" //
				+ "data Clazz as Link Clazz ~ \n" //
				+ "data Clazz as Leaf number ~ \n";

		assertType("number", data //
				+ "let f := v => if-bind (v := Leaf 1) then 1 else if-bind (v := Link Leaf 2) then 2 else 3 ~ \n" //
				+ "f_{Leaf 1} \n");

		assertType("number", data //
				+ "let f := v => \n" //
				+ "    if (v = `Leaf $i`) then i \n" //
				+ "    else if (v = `Link Leaf $i`) then i \n" //
				+ "    else 0 \n" //
				+ "~ \n" //
				+ "f_{Link Leaf 3} \n");
	}

	@Test
	public void testBindList() {
		assertType("number", "let `$h; $t` := 0; 1; ~ h");
		getTypeMustFail("let `$h; $t` := 0; true; ~ h");
	}

	@Test
	public void testClass() {
		assertType("Clazz atom:()", "" //
				+ "data Clazz as Empty ~ \n" //
				+ "define add := (Clazz -> Clazz) of (a => a) ~ \n" //
				+ "add | {Empty}");

		assertType("boolean", "" //
				+ "data T as Nil ~ \n" //
				+ "data T as BTree (T, T) ~ \n" //
				+ "let u := T of Nil ~ \n" //
				+ "let v := T of Nil ~ \n" //
				+ "v = BTree (BTree (Nil, Nil), Nil)");
	}

	@Test
	public void testDefineType() {
		getType("data Weight as Kg number ~ \n" //
				+ "let v := Weight of (Kg 1) ~ \n" //
				+ "v = Kg 99");
		getType("replicate_{23}");
	}

	@Test
	public void testFail() {
		String[] cases = { "1 + \"abc\"" //
				, "2 = true" //
				, "(f => f_{0}) | 1" //
				, "define fib := i2 => dummy => 1; fib_{i2} ~ ()" //
				, "define f := v => (v;) = v ~ f" // cyclic type
				, "use STANDARD ~ define f := erase-type xyz ~ f" //
		};

		// there is a problem in deriving type of 1:(fib_{i2})...
		// rule specified that right hand side of CONS should be a list,
		// however fib_{i2} is a closure.
		for (var c : cases)
			getTypeMustFail(c);
	}

	@Test
	public void testFix() {
		// fix Value Fix None
		// => Fix Value Fix Optional .t
		// => Fix Optional Fix Optional .t
		// => Fix Optional .t (where .t is this type itself)
		System.out.println(getType("" //
				+ "data (Fix (:f, :g)) over some (:f, :g,) as (Fix (:f, Fix (:f, :g))) ~ \n" //
				+ "(:t => Fix Optional :t) of (Fix Value Fix None)"));

		// getType("data (Fix :f) over :f as (Fix (:f, Fix :f)) ~ \n" //
		// + "(:t => Fix Optional :t) of (Fix Value Fix None)");
	}

	@Test
	public void testFun() {
		assertType("number -> number", "a => a + 1");
		assertType("number", "define f := a => a + 1 ~ f_{3}");
		assertType("boolean -> boolean -> boolean", "and");
		assertType("number -> [number]", "v => v; reverse_{1;}");
	}

	@Test
	public void testGeneric() {
		var fp0 = "" //
				+ "data (Rb-tree :t) over :t as Empty ~ \n" //
				+ "define map := (:a => :b => (:a -> :b) -> [:a] -> [:b]) of error () ~ \n" //
				+ "define add := (:t => :t -> Rb-tree :t) of (v => Empty) ~ \n" //
				+ "1; | map_{add} \n";
		assertType("[Rb-tree number]", fp0);

		var fp1 = "" //
				+ "define id := (:t => :t -> :t) of (a => a) ~ (id_{3} + (id_{4;} | head))";
		assertType("number", fp1);
	}

	@Test
	public void testInstance() {
		var define = "" //
				+ "data (List :t) over :t as Nil ~ \n" //
				+ "data (List :t) over :t as Node (:t, List :t) ~ \n" //
				+ "data (List :t) over :t as Node2 (:t, :t, List :t) ~ \n" //
		;

		getType(define + "Nil");
		getType(define + "Node (false, Nil)");
		getType(define + "Node2 (1, 2, Node (3, Nil))");

		assertType("boolean", define //
				+ "let n := Node (true, Nil) ~ Node (false, n) = Nil");

		getTypeMustFail(define + "Node");
		getTypeMustFail(define + "Node 1");
		getTypeMustFail(define + "Node (1, Node (true, Nil))");
		getTypeMustFail(define + "Node2 (1, true, Nil)");
		getTypeMustFail(define + "Node2 (1, 2, Node (true, Nil))");
		getTypeMustFail(define + "Node (1, Nil) = Node (false, Nil)");
		getTypeMustFail(define + "let n := Node (true, Nil) ~ Node (1, n)");
	}

	@Test
	public void testList() {
		assertType("[number]", "1;");
		assertType("[[number]]", "\"a\"; \"b\"; \"c\"; \"d\";");
	}

	@Test
	public void testRbTree() {
		var fps = "use RB-TREE ~ 0 until 10 | map_{dict-insert/_{1}} | apply | {Empty}";
		assertType("Rb-tree (number, number)", fps);
	}

	@Test
	public void testStandard() {
		checkType("use STANDARD ~ ends-with" //
				, "[T] -> _" //
				, "[T] -> [T] -> boolean");
		checkType("use STANDARD ~ join" //
				, "T -> _" //
				, "T -> [[T]] -> [T]");
		checkType("use STANDARD ~ merge" //
				, "([T] -> _) -> _" //
				, "([T] -> [T] -> [T]) -> [T] -> [T]");
	}

	@Test
	public void testTuple() {
		var variant = "" //
				+ "data C as A ~ \n" //
				+ "data C as B number ~ \n" //
				+ "data C as C boolean ~ \n" //
				+ "data CBT as BTree (number, number) ~ \n";

		getType(variant + "A");
		getType(variant + "B 4");
		getType(variant + "C true");
		getType(variant + "if true then A else-if true then (B 3) else (C false)");
		getType(variant + "BTree (2, 3) = BTree (4, 6)");

		getTypeMustFail(variant + "A 4");
		getTypeMustFail(variant + "B");
		getTypeMustFail(variant + "C 0");
		getTypeMustFail("" //
				+ "data C1 as T1 (number, number) ~ \n" //
				+ "data C2 as T2 (number, number) ~ \n" //
				+ "T1 (2, 3) = T2 (2, 3)");
		getTypeMustFail(variant + "BTree (2, 3) = BTree (\"a\", 6)");
	}

	private void checkType(String fps, String bindTo, String ts) {
		var trail = new Trail();
		var type = getType(fps);

		assertTrue(Binder.bind(type, SewingGeneralizerImpl.generalize(Suite.parse(bindTo)), trail));
		assertTrue(Comparer.comparer.compare(Suite.parse(ts), type) == 0);
	}

	private void assertType(String type, String fp) {
		assertEquals(Suite.parse(type), getType(fp));
	}

	private void getTypeMustFail(String fps) {
		try {
			getType(fps);
		} catch (RuntimeException ex) {
			return;
		}
		fail("cannot catch type error of: " + fps);
	}

	private Node getType(String fps) {
		return Suite.evaluateFunType(fps);
	}

}
