package org.instructionexecutor;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.suite.SuiteUtil;
import org.suite.doer.Generalizer;
import org.suite.node.Atom;
import org.suite.node.Node;
import org.suite.node.Reference;

public class FunctionCompilerTypeTest {

	@Test
	public void test() {
		getType("fib = (i1 => i2 => dummy => i2:(fib {i2} {i1 + i2})) >> ()");

		assertEquals(SuiteUtil.parse("LIST-OF NUMBER") //
				, getType("1:"));
		assertEquals(SuiteUtil.parse("LIST-OF STRING") //
				, getType("\"a\":\"b\":\"c\":\"d\":"));
		assertEquals(SuiteUtil.parse("BOOLEAN") //
				, getType("4 = 8"));
		assertEquals(SuiteUtil.parse("CALLABLE NUMBER NUMBER") //
				, getType("a => a + 1"));
		assertEquals(SuiteUtil.parse("NUMBER") //
				, getType("f = (a => a + 1) >> f {3}"));
	}

	private static Node getType(String f) {
		Node program = SuiteUtil.parse(f);

		Node node = SuiteUtil
				.parse("parse-fc .program .p, infer-type .p _ .type");

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
