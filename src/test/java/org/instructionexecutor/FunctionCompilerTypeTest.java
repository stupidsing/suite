package org.instructionexecutor;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.suite.Binder;
import org.suite.Journal;
import org.suite.SuiteUtil;
import org.suite.doer.Generalizer;
import org.suite.node.Atom;
import org.suite.node.Node;
import org.suite.node.Reference;

public class FunctionCompilerTypeTest {

	@Test
	public void test() {
		assertEquals(SuiteUtil.parse("LIST-OF NUMBER") //
				, getType("1:"));
		assertEquals(SuiteUtil.parse("LIST-OF STRING") //
				, getType("\"a\":\"b\":\"c\":\"d\":"));
		assertEquals(SuiteUtil.parse("BOOLEAN") //
				, getType("4 = 8"));
		assertEquals(SuiteUtil.parse("FUNC NUMBER NUMBER") //
				, getType("a => a + 1"));
		assertEquals(SuiteUtil.parse("NUMBER") //
				, getType("f = (a => a + 1) >> f {3}"));
		assertTrue(Binder.bind(SuiteUtil.parse("FUNC _ (CO-LIST-OF NUMBER)") //
				, getType("fib = (n => dummy => n/(fib {n + 1})) >> \n" //
						+ "fib {1}") // Pretends co-recursion
				, new Journal()));
	}

	@Test
	public void testFail() {
		String cases[] = { "1 + 'abc'" //
				, "fib = (i2 => dummy => 1:(fib {i2})) >> ()" };

		// There is a problem in deriving type of 1:(fib {i2})...
		// Rule specified that right hand side of CONS should be a list,
		// however fib {i2} is a closure.
		// Should actually use corecursive list type (cons-ed by '^').
		for (String c : cases) {
			try {
				getType(c);
			} catch (RuntimeException ex) {
				continue;
			}
			throw new RuntimeException("Cannot catch type error of: " + c);
		}
	}

	private static Node getType(String f) {
		Node program = SuiteUtil.parse(f);

		Node node = SuiteUtil
				.parse("parse-fc .program .p, infer-type .p () .type");

		Generalizer generalizer = new Generalizer();
		node = generalizer.generalize(node);
		Node variable = generalizer.getVariable(Atom.create(".program"));
		Node type = generalizer.getVariable(Atom.create(".type"));

		((Reference) variable).bound(program);

		String[] imports = { "auto.sl", "fc.sl" };
		if (SuiteUtil.getProver(imports).prove(node))
			return type.finalNode();
		else
			throw new RuntimeException("Type inference error");
	}

}
