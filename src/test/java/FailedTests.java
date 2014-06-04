import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;

import org.junit.Test;

import suite.Suite;
import suite.fp.eval.FunRbTreeTest;
import suite.lp.eval.LogicCompilerLevel1Test;
import suite.lp.kb.RuleSet;

public class FailedTests {

	// Cannot bind external symbols when using is used in a closure
	@Test
	public void testClosureUsing() {
		Suite.evaluateFun("using STANDARD >> id {using MONAD >> 1}", true);
	}

	@Test
	public void testMemberOfMember() {
		new LogicCompilerLevel1Test().testMemberOfMember();
	}

	// (Expected) infinite loop.
	// (Actual) short boolean evaluation in Prover skipped the loop:
	// alt = andTree(bt, orTree(andTree(right, rem), alt));
	@Test
	public void testRepeat() throws IOException {
		RuleSet rs = Suite.createRuleSet();
		Suite.importResource(rs, "auto.sl");
		assertTrue(Suite.proveLogic(rs, "repeat, fail"));
	}

	// Why returning null pointer?
	@Test
	public void testRecursiveCall() {
		assertNotNull(Suite.evaluateFun("define f := f >> f", false));
		assertNotNull(Suite.evaluateFun("define f := f >> f", true));
	}

	// Takes forever to type check
	// @Test
	public void testRecursiveType() {
		Suite.evaluateFunType("data (rb-tree {:t}) over :t as (rb-tree {:t}) >> (rb-tree {:t}) of 1");
	}

	// Takes 11 seconds to type check
	@Test
	public void testTypeCheck() throws IOException {
		new FunRbTreeTest().test();
	}

}
