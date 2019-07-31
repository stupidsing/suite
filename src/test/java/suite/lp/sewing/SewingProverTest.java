package suite.lp.sewing;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import primal.fp.Funs.Source;
import suite.Suite;
import suite.lp.Configuration.ProverCfg;
import suite.lp.doer.Generalizer;
import suite.lp.kb.Rule;
import suite.lp.sewing.impl.SewingProverImpl;
import suite.node.Atom;
import suite.node.Int;
import suite.node.Tree;
import suite.node.io.TermOp;
import suite.node.tree.TreeTuple;
import suite.os.Stopwatch;

public class SewingProverTest {

	@Test
	public void test() {
		var rs = Suite.newRuleSet();
		Suite.addRule(rs, "yes");

		var sp = new SewingProverImpl(rs);
		var pc = new ProverCfg(rs);
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
		var rs = Suite.newRuleSet();
		Suite.addRule(rs, "mem (.e, _) .e");
		Suite.addRule(rs, "mem (_, .tail) .e :- mem .tail .e");
		Suite.addRule(rs, "q .c .v :- once (mem (0,) .v), .a/.b/.c = 0/0/0; mem (1,) .v, .a/.b/.c = 1/1/1");
		Suite.addRule(rs, "r .c :- q .c .v, .v = 1");

		var sp = new SewingProverImpl(rs);
		var pc = new ProverCfg(rs);
		assertTrue(sp.prover(new Generalizer().generalize(Suite.parse("r .c"))).test(pc));
	}

	@Test
	public void testCut() {
		var rs = Suite.newRuleSet();
		Suite.addRule(rs, "a :- b");
		Suite.addRule(rs, "a");
		Suite.addRule(rs, "b :- !, fail");

		var sp = new SewingProverImpl(rs);
		var pc = new ProverCfg(rs);
		assertTrue(sp.prover(Suite.parse("a")).test(pc));
	}

	@Test
	public void testEnv() {
		var rs = Suite.newRuleSet();
		Suite.addRule(rs, "a :- b .a, b .b");
		Suite.addRule(rs, "b 1");

		var sp = new SewingProverImpl(rs);
		var pc = new ProverCfg(rs);
		assertTrue(sp.prover(Suite.parse("a")).test(pc));
	}

	@Test
	public void testIf() {
		var rs = Suite.newRuleSet();
		var sp = new SewingProverImpl(rs);
		var pc = new ProverCfg(rs);
		assertTrue(sp.prover(Suite.parse("if () () fail")).test(pc));
		assertFalse(sp.prover(Suite.parse("if () fail ()")).test(pc));
		assertTrue(sp.prover(Suite.parse("if fail fail ()")).test(pc));
		assertFalse(sp.prover(Suite.parse("if fail () fail")).test(pc));
	}

	@Test
	public void testPerformance() {
		var rs = Suite.newRuleSet();
		var pred = Atom.of("q");
		var tail = Atom.NIL;

		for (var i = 0; i < 65536; i++)
			rs.addRule(Rule.of(Tree.of(TermOp.IS____, TreeTuple.of(pred, Int.of(i)), tail)));

		var sp = new SewingProverImpl(rs);
		var pc = new ProverCfg(rs);
		var test = sp.prover(Suite.parse("q 32768"));

		Source<Stopwatch<Boolean>> trial = () -> Stopwatch.of(() -> {
			var isOk = true;
			for (var i = 0; i < 65536; i++)
				isOk &= test.test(pc);
			assertTrue(isOk);
			return isOk;
		});

		for (var i = 0; i < 8; i++)
			trial.g();

		var sw = trial.g();

		System.out.println(sw.duration);
		assertTrue(sw.duration < 300);
	}

}
