package org.fp;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.suite.doer.TermParser;
import org.suite.node.Atom;
import org.suite.node.Int;
import org.suite.node.Node;

public class InterpreterTest {

	private TermParser parser = new TermParser();
	private Interpreter interpreter = new Interpreter();

	@Test
	public void testUnitize() {
		addFunction("unitize", "value => if (value = 0) then 0 else 1");
		assertEquals(Int.create(0), evaluate("unitize / 0"));
	}

	@Test
	public void testSign() {
		addFunction("sign", "x => switch ((x < 0 => -1) (x > 0 => 1) 0)");
		assertEquals(Int.create(-1), evaluate("sign / -100"));
		assertEquals(Int.create(0), evaluate("sign / 0"));
		assertEquals(Int.create(1), evaluate("sign / 100"));
	}

	@Test
	public void testFibonacci() {
		addFunction("fib",
				"n => if (n = 0; n = 1) then 1 else (fib / (n - 1) + fib / (n - 2))");
		assertEquals(Int.create(89), evaluate("fib / 10"));
	}

	@Test
	public void testAddFunctions() {
		addFunction(
				"member",
				"list => item => switch ("
						+ " (tree / list, \",\" = oper / list) =>"
						+ " if (item = left / list) true else (member / (right list) / item)"
						+ " ) false)");

		addFunction("join", "f1 => f2 => in => (f1 / (f2 / in))");
	}

	private void addFunction(String head, String body) {
		interpreter.addFunction(Atom.create(head), parser.parse(body));
	}

	private Node evaluate(String expression) {
		return interpreter.evaluate(parser.parse(expression));
	}

}
