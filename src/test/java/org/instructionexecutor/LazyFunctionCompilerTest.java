package org.instructionexecutor;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.junit.Test;
import org.suite.SuiteUtil;
import org.suite.doer.Generalizer;
import org.suite.node.Atom;
import org.suite.node.Int;
import org.suite.node.Node;
import org.suite.node.Reference;
import org.suite.node.Tree;

public class LazyFunctionCompilerTest {

	@Test
	public void testFibonacci() {
		assertEquals(Int.create(1), eval("" //
				+ "define fib = (i1 => i2 => i2, fib {i2} {i1 + i2}) >> \n" //
				+ "head {fib {0} {1}}"));

		assertEquals(Int.create(89), eval("" // Real co-recursion!
				+ "define fib = (i1 => i2 => i2, fib {i2} {i1 + i2}) >> \n" //
				+ "define h = (f => head {f}) >> \n" //
				+ "define t = (f => tail {f}) >> \n" //
				+ "apply {fib {0} {1}} {t, t, t, t, t, t, t, t, t, t, h,}"));
	}

	@Test
	public void testFilter() {
		assertEquals(SuiteUtil.parse("4,") //
				, eval("(item => list => true ? (), list | list) {1} {}"));
	}

	@Test
	public void testSubstitution() {
		assertEquals(Int.create(8), eval("define v = 4 >> v + v"));
	}

	@Test
	public void testSystem() {
		assertNotNull(Tree.decompose(eval("" //
				+ "define if-tree = (f1 => f1 {135}) >> \n" //
				+ "cons {1} {}")));
		assertEquals(SuiteUtil.parse("1"), eval("" //
				+ "head {1, 2, 3,}"));
		assertEquals(SuiteUtil.parse("2, 3,"), eval("" //
				+ "tail {1, 2, 3,}"));
	}

	private static Node eval(String f) {
		Node node = SuiteUtil.parse("compile-function .program .code, pp-list .code");

		Generalizer generalizer = new Generalizer();
		node = generalizer.generalize(node);
		Node variable = generalizer.getVariable(Atom.create(".program"));
		Node ics = generalizer.getVariable(Atom.create(".code"));

		((Reference) variable).bound(SuiteUtil.parse(f));

		String imports[] = { "auto.sl", "fc.sl" };
		if (SuiteUtil.getProver(imports).prove(node))
			return new FunctionInstructionExecutor(ics).execute();
		else
			throw new RuntimeException("Function compilation error");
	}

}
