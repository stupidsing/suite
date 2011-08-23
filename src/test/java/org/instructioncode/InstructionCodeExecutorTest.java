package org.instructioncode;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;

import org.junit.Test;
import org.suite.SuiteUtil;
import org.suite.doer.Generalizer;
import org.suite.doer.Prover;
import org.suite.kb.RuleSet;
import org.suite.node.Atom;
import org.suite.node.Int;
import org.suite.node.Node;
import org.suite.node.Tree;

public class InstructionCodeExecutorTest {

	@Test
	public void testClosure() throws IOException {
		assertEquals(Int.create(7), run("" //
				+ "add = (p => q => p + q) >> add {3} {4}"));
		assertEquals(Int.create(20), run("" //
				+ "p = (n => n + 1) >> \n" //
				+ "q = (n => p {n} * 2) >> \n" //
				+ "q {9}"));
	}

	@Test
	public void testJoin() throws IOException {
		assertEquals(Int.create(19), run("" //
				+ "join = (f => g => x => f {g {x}}) >> \n" //
				+ "p = (n => n + 1) >> \n" //
				+ "q = (n => n * 2) >> \n" //
				+ "r = (join {p} {q}) >> \n" //
				+ "r {9}"));
	}

	@Test
	public void testFibonacci() throws IOException {
		assertEquals(Int.create(89), run("" //
				+ "fib = (n => \n" //
				+ "    n > 1 \n" //
				+ "    ? fib {n - 1} + fib {n - 2} \n" //
				+ "    | 1 \n" //
				+ ") >> \n" //
				+ "fib {10}"));
	}

	@Test
	public void testIf() throws IOException {
		assertEquals(Int.create(0), run("3 > 4 ? 1 | 0"));
		assertEquals(Int.create(1), run("3 = 3 ? 1 | 0"));
	}

	@Test
	public void testSys() throws IOException {
		assertNotNull(Tree.decompose(run("cons {1} {2:}")));
		assertEquals(Int.create(1), run("head {1:2:3:}"));
		assertNotNull(Tree.decompose(run("tail {1:2:3:}")));
	}

	private Node run(String program) throws IOException {
		RuleSet rs = new RuleSet();
		SuiteUtil.importResource(rs, "auto.sl");
		SuiteUtil.importResource(rs, "fc.sl");

		Node node = SuiteUtil.parse("" //
				+ "compile (\n" + program + "\n) .c/.reg \n" //
				+ ", pp-list .c");

		Generalizer generalizer = new Generalizer();
		node = generalizer.generalize(node);
		assertTrue(new Prover(rs).prove(node));

		Node ics = generalizer.getVariable(Atom.create(".c"));
		return new InstructionCodeExecutor(ics).execute();
	}

}
