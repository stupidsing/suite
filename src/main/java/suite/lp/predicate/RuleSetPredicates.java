package suite.lp.predicate;

import static suite.util.Fail.fail;

import java.util.ArrayList;

import suite.Suite;
import suite.lp.doer.Binder;
import suite.lp.doer.Prover;
import suite.lp.kb.CompositeRuleSet;
import suite.lp.kb.Prototype;
import suite.lp.kb.Rule;
import suite.lp.predicate.PredicateUtil.BuiltinPredicate;
import suite.node.Node;
import suite.node.Tree;
import suite.node.io.Formatter;
import suite.node.io.TermOp;
import suite.node.pp.PrettyPrinter;
import suite.node.util.TreeUtil;

public class RuleSetPredicates {

	public BuiltinPredicate asserta = PredicateUtil.p1((prover, p0) -> {
		prover.ruleSet().addRuleToFront(Rule.of(p0));
		return true;
	});

	public BuiltinPredicate assertz = PredicateUtil.p1((prover, p0) -> {
		prover.ruleSet().addRule(Rule.of(p0));
		return true;
	});

	public BuiltinPredicate getAllRules = PredicateUtil.p1((prover, p0) -> {
		var ruleSet = prover.ruleSet();
		var rules = ruleSet.getRules();
		var nodes = new ArrayList<Node>();

		for (var rule : rules)
			nodes.add(Tree.of(TermOp.IS____, rule.head, rule.tail));

		return prover.bind(TreeUtil.buildUp(TermOp.NEXT__, nodes), p0);
	});

	public BuiltinPredicate importPredicate = PredicateUtil.p1((prover, p0) -> prover.ruleSet().importFrom(p0));

	public BuiltinPredicate importUrl = PredicateUtil.p1((prover, p0) -> {
		var url = Formatter.display(p0);
		try {
			return prover.ruleSet().importUrl(url);
		} catch (Exception ex) {
			return fail("exception when importing " + url, ex);
		}
	});

	public BuiltinPredicate list = PredicateUtil.ps((prover, ps) -> {
		Prototype proto = null;
		if (0 < ps.length)
			proto = Prototype.of(ps[0]);

		Node node = Suite.listRules(prover.ruleSet(), proto);
		var printer = new PrettyPrinter();
		System.out.println(printer.prettyPrint(node));
		return true;
	});

	public BuiltinPredicate retract = PredicateUtil.p1((prover, p0) -> {
		prover.ruleSet().removeRule(Rule.of(p0));
		return true;
	});

	public BuiltinPredicate retractAll = PredicateUtil.p1((prover, p0) -> {
		var rule0 = Rule.of(p0);

		var ruleSet = prover.ruleSet();
		var trail = prover.getTrail();
		var pit = trail.getPointInTime();
		var targets = new ArrayList<Rule>();

		for (var rule : ruleSet.getRules()) {
			if (Binder.bind(rule0.head, rule.head, trail) //
					&& Binder.bind(rule0.tail, rule.tail, trail))
				targets.add(rule);

			trail.unwind(pit);
		}

		for (var rule : targets)
			ruleSet.removeRule(rule);

		return true;
	});

	public BuiltinPredicate with = PredicateUtil.p2((prover, p0, p1) -> {
		var ruleSet0 = prover.ruleSet();
		var ruleSet1 = Suite.getRuleSet(p0);
		var ruleSet2 = new CompositeRuleSet(ruleSet1, ruleSet0);
		return new Prover(ruleSet2).prove(p1);
	});

}
