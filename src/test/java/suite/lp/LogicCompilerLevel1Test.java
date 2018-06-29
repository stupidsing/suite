package suite.lp;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.junit.Test;

import suite.Suite;
import suite.lp.Configuration.ProverCfg;
import suite.lp.doer.Specializer;
import suite.lp.kb.RuleSet;
import suite.lp.search.CompiledProverBuilder;
import suite.lp.search.ProverBuilder.Finder;
import suite.node.Atom;
import suite.node.Node;

public class LogicCompilerLevel1Test {

	/**
	 * Compiles the functional compiler and use it to compile a simple functional
	 * program.
	 */
	@Test
	public void testCompileFunProgram() {
		var rs = Suite.newRuleSet(List.of("auto.sl", "fc/fc.sl"));
		var gs = "" //
				+ "source .in" //
				+ ", compile-function .0 .in .out" //
				+ ", sink .out";

		var goal = new Specializer().specialize(Suite.substitute(gs, Atom.of("LAZY")));
		var input = Suite.parse("1 + 2");
		var result = finder(rs, goal).collectSingle(input);

		System.out.println(result);
		assertNotNull(result);
	}

	@Test
	public void testMemberOfMember() {
		var rs = Suite.newRuleSet(List.of("auto.sl"));
		var goal = Suite.parse("source .lln, member .lln .ln, member .ln .n, sink .n");
		var input = Suite.parse("((1, 2,), (3, 4,),)");
		var results = finder(rs, goal).collectList(input);

		System.out.println(results);
		assertTrue(results.size() == 4);
	}

	/**
	 * This test might fail in some poor tail recursion optimization
	 * implementations, as some variables are not unbounded when backtracking.
	 */
	@Test
	public void testTailCalls() {
		var rs = Suite.newRuleSet();
		Suite.addRule(rs, "ab a");
		Suite.addRule(rs, "ab b");

		var goal = Suite.parse("ab .a, ab .b, sink (.a, .b,)");
		var results = finder(rs, goal).collectList(Atom.NIL);

		System.out.println(results);
		assertTrue(results.size() == 4);
	}

	private Finder finder(RuleSet rs, Node goal) {
		return CompiledProverBuilder.level1(new ProverCfg()).build(rs).apply(goal);
	}

}
