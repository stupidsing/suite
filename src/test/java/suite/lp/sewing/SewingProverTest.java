package suite.lp.sewing;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import suite.Suite;
import suite.lp.Configuration.ProverConfig;
import suite.lp.doer.Generalizer;
import suite.lp.doer.ProverFactory;
import suite.lp.doer.ProverFactory.Prove_;
import suite.lp.kb.Rule;
import suite.lp.kb.RuleSet;
import suite.lp.sewing.impl.SewingProverImpl;
import suite.node.Atom;
import suite.node.Int;
import suite.node.Tree;
import suite.node.io.TermOp;
import suite.os.Stopwatch;
import suite.util.FunUtil.Source;

public class SewingProverTest {

	@Test
	public void test() {
		RuleSet rs = Suite.newRuleSet();
		Suite.addRule(rs, "yes");

		ProverFactory sp = new SewingProverImpl(rs);
		ProverConfig pc = new ProverConfig(rs);
		assertTrue(sp.prover(Suite.parse("yes")).test(pc));
		assertTrue(sp.prover(Suite.parse("fail; yes")).test(pc));
		assertTrue(sp.prover(Suite.parse("yes; yes")).test(pc));
		assertFalse(sp.prover(Suite.parse("yes, fail")).test(pc));

		assertFalse(sp.prover(Suite.parse("!, fail; yes")).test(pc));

		assertTrue(sp.prover(new Generalizer().generalize(Suite.parse("(.v = 1; .v = 2), .v = 2"))).test(pc));
		assertFalse(sp.prover(new Generalizer().generalize(Suite.parse("once (.v = 1; .v = 2), .v = 2"))).test(pc));

		assertFalse(sp.prover(Suite.parse("not yes")).test(pc));
		assertTrue(sp.prover(Suite.parse("not fail")).test(pc));

		assertTrue(sp.prover(new Generalizer().generalize(Suite.parse(".p = yes, .p"))).test(pc));
	}

	@Test
	public void testBacktrack() {
		RuleSet rs = Suite.newRuleSet();
		Suite.addRule(rs, "mem (.e, _) .e");
		Suite.addRule(rs, "mem (_, .tail) .e :- mem .tail .e");
		Suite.addRule(rs, "q .c .v :- once (mem (0,) .v), .a/.b/.c = 0/0/0; mem (1,) .v, .a/.b/.c = 1/1/1");
		Suite.addRule(rs, "r .c :- q .c .v, .v = 1");

		ProverFactory sp = new SewingProverImpl(rs);
		ProverConfig pc = new ProverConfig(rs);
		assertTrue(sp.prover(new Generalizer().generalize(Suite.parse("r .c"))).test(pc));
	}

	@Test
	public void testCut() {
		RuleSet rs = Suite.newRuleSet();
		Suite.addRule(rs, "a :- b");
		Suite.addRule(rs, "a");
		Suite.addRule(rs, "b :- !, fail");

		ProverFactory sp = new SewingProverImpl(rs);
		ProverConfig pc = new ProverConfig(rs);
		assertTrue(sp.prover(Suite.parse("a")).test(pc));
	}

	@Test
	public void testEnv() {
		RuleSet rs = Suite.newRuleSet();
		Suite.addRule(rs, "a :- b .a, b .b");
		Suite.addRule(rs, "b 1");

		ProverFactory sp = new SewingProverImpl(rs);
		ProverConfig pc = new ProverConfig(rs);
		assertTrue(sp.prover(Suite.parse("a")).test(pc));
	}

	@Test
	public void testIf() {
		RuleSet rs = Suite.newRuleSet();
		ProverFactory sp = new SewingProverImpl(rs);
		ProverConfig pc = new ProverConfig(rs);
		assertTrue(sp.prover(Suite.parse("if () () fail")).test(pc));
		assertFalse(sp.prover(Suite.parse("if () fail ()")).test(pc));
		assertTrue(sp.prover(Suite.parse("if fail fail ()")).test(pc));
		assertFalse(sp.prover(Suite.parse("if fail () fail")).test(pc));
	}

	@Test
	public void testPerformance() {
		RuleSet rs = Suite.newRuleSet();
		Atom pred = Atom.of("q");
		Atom tail = Atom.NIL;

		for (int i = 0; i < 65536; i++)
			rs.addRule(Rule.of(Tree.of(TermOp.IS____, Tree.of(TermOp.TUPLE_, pred, Int.of(i)), tail)));

		ProverFactory sp = new SewingProverImpl(rs);
		ProverConfig pc = new ProverConfig(rs);
		Prove_ test = sp.prover(Suite.parse("q 32768"));

		Source<Stopwatch<Boolean>> trial = () -> Stopwatch.of(() -> {
			boolean isOk = true;
			for (int i = 0; i < 65536; i++)
				isOk &= test.test(pc);
			assertTrue(isOk);
			return isOk;
		});

		for (int i = 0; i < 8; i++)
			trial.source();

		Stopwatch<Boolean> sw = trial.source();

		System.out.println(sw.duration);
		assertTrue(sw.duration < 300);
	}

}
