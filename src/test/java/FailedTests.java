import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

import org.junit.Test;

import suite.Suite;
import suite.fp.FunRbTreeTest;
import suite.fp.MonadTest;
import suite.lp.kb.RuleSet;

public class FailedTests {

	// Duplicate symbols. Cannot bind again when using is used in a closure
	@Test
	public void testClosureUsing() {
		List<String> libraries0 = Suite.libraries;
		Suite.libraries = Collections.emptyList();
		try {
			Suite.evaluateFun("using MATH >> (a => (using MATH >> 1)) {0}", true);
		} finally {
			Suite.libraries = libraries0;
		}
	}

	// Out of memory. Caused by Tree.forceSetRight(tree, null) in
	// ThunkUtil.yawnSource()
	@Test
	public void testConcatm() throws IOException {
		new MonadTest().testConcatm();
	}

	// NPE. concatm may not be correctly linked with standard library.
	// Module dependency checks are necessary
	@Test
	public void testEager() {
		List<String> libraries0 = Suite.libraries;
		Suite.libraries = Collections.emptyList();
		try {
			Suite.evaluateFun("using MONAD >> 0", false);
		} finally {
			Suite.libraries = libraries0;
		}
	}

	// (Expected) infinite loop.
	// (Actual) short boolean evaluation in Prover skipped the loop:
	// alt = andTree(bt, orTree(andTree(right, rem), alt));
	@Test
	public void testRepeat() throws IOException {
		RuleSet rs = Suite.createRuleSet();
		Suite.importPath(rs, "auto.sl");
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
