package org.fp;

import static org.junit.Assert.assertEquals;

import java.io.IOException;

import org.junit.Before;
import org.junit.Test;
import org.suite.doer.TermParser;
import org.suite.node.Atom;
import org.suite.node.Int;
import org.suite.node.Node;

public class InterpreterTest {

	private TermParser parser = new TermParser();
	private Interpreter interpreter = new Interpreter();

	@Before
	public void before() throws IOException {
		interpreter.addFunctions(parser.parseClassPathFile("auto.fp"));
	}

	@Test
	public void testStringManipulation() {
		assertEquals(parse("\"a\""), evaluate("h {\"abc\"}"));
		assertEquals(parse("\"bc\""), evaluate("t {\"abc\"}"));
	}

	@Test
	public void testMap() {
		assertEquals(parse("(2, 3, 4,)"), //
				evaluate("map {a => a + 1} {1, 2, 3,}"));
	}

	@Test
	public void testJoin() {
		assertEquals(parse("4"), evaluate("join {a => a + 1} {b => b + 1} {2}"));
	}

	@Test
	public void testAndOr() {
		assertEquals(parse("false"), evaluate("and {false} {true}"));
		assertEquals(parse("true"), evaluate("or {false} {true}"));
	}

	@Test
	public void testFilter() {
		assertEquals(parse("2, 2,"),
				evaluate("filter {e => e = 2} {1, 2, 3, 2,}"));
	}

	@Test
	public void testMemberOf() {
		assertEquals(parse("true"), evaluate("member-of {3} {1, 3,}"));
	}

	@Test
	public void testFold() {
		assertEquals(parse("false"), evaluate("fold {and} {true, false,}"));
		assertEquals(parse("true"), evaluate("fold {and} {true, true,}"));
	}

	@Test
	public void testUnitize() {
		addFunction("unitize", "value => (value = 0 ? 0 | 1)");
		assertEquals(Int.create(0), evaluate("unitize {0}"));
	}

	@Test
	public void testSign() {
		addFunction("sign", "x => (x < 0 ? -1 | x > 0 ? 1 | 0)");
		assertEquals(Int.create(-1), evaluate("sign {-100}"));
		assertEquals(Int.create(0), evaluate("sign {0}"));
		assertEquals(Int.create(1), evaluate("sign {100}"));
	}

	@Test
	public void testLet() {
		assertEquals(Int.create(5), evaluate("x = 5 >> x"));
	}

	@Test
	public void testAdd() {
		addFunction("add", "x => y => x + y");
		assertEquals(Int.create(5), evaluate("add {2} {3}"));
	}

	@Test
	public void testIf() {
		assertEquals(Atom.create("p"), evaluate("if {1 = 1} {p} {q}"));
		assertEquals(Atom.create("q"), evaluate("if {0 = 1} {p} {q}"));
	}

	@Test
	public void testFibonacci() {
		addFunction("fib",
				"n => (or {n = 0} {n = 1} ? 1 | fib {n - 1} + fib {n - 2})");
		assertEquals(Int.create(89), evaluate("fib {10}"));
	}

	@Test
	public void testConcat() {
		assertEquals(parse("a, b, c, d, e,"),
				evaluate("concat {a, b, c,} {d, e,}"));
	}

	@Test
	public void testConcatLists() {
		assertEquals(parse("a, b, c, d, e,"),
				evaluate("concat-lists {(a, b, c,), (), (d, e,),}"));
	}

	private void addFunction(String head, String body) {
		interpreter.addFunction(Atom.create(head), parse(body));
	}

	private Node evaluate(String expression) {
		return interpreter.evaluate(parse(expression));
	}

	private Node parse(String expression) {
		return parser.parse(expression);
	}

}
