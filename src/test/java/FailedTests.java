import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;

import org.junit.Test;

import suite.Suite;
import suite.fp.eval.FunRbTreeTest;
import suite.lp.doer.Configuration.ProverConfig;
import suite.lp.doer.Specializer;
import suite.lp.kb.Rule;
import suite.lp.kb.RuleSet;
import suite.lp.search.CompiledProverBuilder;
import suite.lp.search.FindUtil;
import suite.node.Int;
import suite.node.Node;

public class FailedTests {

	// Cannot bind external symbols when using is used in a closure
	@Test
	public void testClosureUsing() {
		Suite.evaluateFun("using STANDARD >> id {using MONAD >> 1}", true);
	}

	@Test
	public void testCompileFunProgram() throws IOException {
		RuleSet rs = Suite.createRuleSet();
		rs.addRule(Rule.formRule(Suite.parse("test :- (test; nl), test")));
	
		Node goal = new Specializer().specialize(Suite.substitute("test"));
		FindUtil.collectSingle(CompiledProverBuilder.level1(new ProverConfig()).build(rs, goal), Int.of(1));
	}

	@Test
	public void testCutTailRecursion() {
		assertTrue(Suite.proveLogic("(dec 0 :- ! # dec .n :- let .n1 (.n - 1), dec .n1, ! #) >> dec 65536"));
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

}
