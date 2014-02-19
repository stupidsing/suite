import static org.junit.Assert.assertTrue;

import java.io.IOException;

import org.junit.Test;

import suite.Suite;
import suite.fp.eval.FunRbTreeTest;
import suite.lp.kb.RuleSet;

public class FailedTests {

	// Type check take 11 seconds
	@Test
	public void testTypeCheck() throws IOException {
		new FunRbTreeTest().test();
	}

	// Cyclic types
	@Test
	public void testCyclicType() {
		Suite.evaluateFunType("define f = (v => (v;) = v) >> f");
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

	// Impossible type
	@Test
	public void testImpossibleType() {
		Suite.evaluateFunType("let `$n; $p;` = (true; 3;) >> p");
		assertTrue(false);
	}

}
