package org.instructioncode;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.IOException;

import org.junit.Test;
import org.suite.SuiteUtil;
import org.suite.doer.Generalizer;
import org.suite.doer.Prover;
import org.suite.kb.RuleSet;
import org.suite.node.Atom;
import org.suite.node.Node;

public class InstructionCodeExecutorTest {

	@Test
	public void testClosure() throws IOException {
		assertEquals(7, run("add = (p => q => p + q) >> add {3} {4}"));
	}

	@Test
	public void testIf() throws IOException {
		assertEquals(0, run("3 > 4 ? 1 | 0"));
		assertEquals(1, run("3 = 3 ? 1 | 0"));
	}

	private int run(String program) throws IOException {
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
