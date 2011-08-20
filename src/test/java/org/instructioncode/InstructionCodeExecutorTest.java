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
	public void test() throws IOException {
		String program = "" //
				+ "g = (p => q => p + q) >> \n" //
				+ "g {3} {4}";

		RuleSet rs = new RuleSet();
		SuiteUtil.importResource(rs, "auto.sl");
		SuiteUtil.importResource(rs, "fc.sl");

		Node node = SuiteUtil.parse("" //
				+ "compile ( \n" + program + "\n) .c/.reg \n" //
				+ ", pp-list .c");

		Generalizer generalizer = new Generalizer();
		node = generalizer.generalize(node);
		Prover prover = new Prover(rs);

		assertTrue(prover.prove(node));

		Node ics = generalizer.getVariable(Atom.create(".c"));
		assertEquals(7, new InstructionCodeExecutor(ics).execute());
	}

}
