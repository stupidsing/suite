import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.ArrayList;

import org.junit.Test;

import suite.Suite;
import suite.fp.FunRbTreeTest;
import suite.lp.kb.RuleSet;

public class FailedTests {

	// Duplicate symbols. Cannot bind again when using is used in a closure
	@Test
	public void testClosureUsing() {
		Suite.libraries = new ArrayList<>();
		Suite.evaluateFun("using MATH >> (a => (using MATH >> 1)) {0}", true);
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
		long start = System.currentTimeMillis();
		new FunRbTreeTest().test();
		long end = System.currentTimeMillis();
		assertTrue(end - start < 1000l);
	}

	// Undefined variable not showing appropriate error message
	@Test
	public void testVariableNotFound() {
		Suite.evaluateFun("define i := skip-type-check abc >> i", false);
		assertFalse(true);
	}

}
