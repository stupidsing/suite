import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;

import org.junit.Test;

import suite.Suite;
import suite.fp.FunRbTreeTest;
import suite.lp.kb.RuleSet;
import suite.node.Node;
import suite.node.io.Formatter;
import suite.streamlet.Read;
import suite.util.Util;

public class FailedTests {

	// Why a flat tree?
	@Test
	public void test23Tree() {
		int n = 11;
		Node fp1 = Suite.substitute("using 23-TREE >> " //
				+ "0 until " + (n / 2) + " " //
				+ "| map {remove} " //
				+ "| apply " //
				+ "| {0 until " + n + " | map {insert} | apply | {Tree (9999, Empty;)}}");
		Node result1 = Suite.evaluateFun(Suite.fcc(fp1, false));
		String s = Formatter.dump(result1);
		System.out.println("OUT:\n" + s);
		int nPars = Read.from(Util.chars(s)).filter(c -> c == '(').size();
		assertTrue(nPars >= 3);
	}

	// Duplicate symbols. Cannot bind again when using is used in a closure
	@Test
	public void testClosureUsing() {
		Suite.applyNoLibraries(() -> Suite.evaluateFun("using MATH >> (a => (using MATH >> 1)) {0}", true));
	}

	// NPE. Method not found in concatm due to not importing standard library.
	// Module dependency checks are necessary
	@Test
	public void testEager() {
		Suite.applyNoLibraries(() -> Suite.evaluateFun("using MONAD >> 0", false));
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
		Suite.isInstructionDump = true;
		Suite.isInstructionTrace = true;
		Suite.applyNoLibraries(() -> {
			assertNotNull(Suite.evaluateFun("define f := f >> f", true));
			return true;
		});
	}

	// Takes forever to type check
	// @Test
	public void testRecursiveType() {
		Suite.evaluateFunType("data (rb-tree {:t}) over :t as (rb-tree {:t}) >> (:t => rb-tree {:t}) of 1");
	}

	// Takes 11 seconds to type check
	@Test
	public void testTypeCheck() throws IOException {
		long start = System.currentTimeMillis();
		new FunRbTreeTest().test();
		long end = System.currentTimeMillis();
		assertTrue(end - start < 1000l);
	}

}
