package org.instructionexecutor;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.suite.SuiteUtil;
import org.suite.doer.Generalizer;
import org.suite.doer.Prover;
import org.suite.node.Atom;
import org.suite.node.Int;
import org.suite.node.Node;
import org.suite.node.Reference;

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

	private static Node eval(String f) {
		Node node = SuiteUtil.parse("compile-function .program .code");

		Generalizer generalizer = new Generalizer();
		node = generalizer.generalize(node);
		Node variable = generalizer.getVariable(Atom.create(".program"));
		Node ics = generalizer.getVariable(Atom.create(".code"));

		((Reference) variable).bound(SuiteUtil.parse(f));

		String imports[] = { "auto.sl", "fc.sl" };
		Prover prover = SuiteUtil.getProver(imports);
		prover.getRuleSet().addRule(SuiteUtil.parse("fc-is-lazy"));

		if (prover.prove(node))
			return new FunctionInstructionExecutor(ics).execute();
		else
			throw new RuntimeException("Function compilation error");
	}

}
