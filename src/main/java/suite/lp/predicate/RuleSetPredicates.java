package suite.lp.predicate;

import java.util.ArrayList;
import java.util.List;

import suite.Suite;
import suite.lp.Trail;
import suite.lp.doer.Binder;
import suite.lp.doer.Prover;
import suite.lp.kb.CompositeRuleSet;
import suite.lp.kb.Prototype;
import suite.lp.kb.Rule;
import suite.lp.kb.RuleSet;
import suite.lp.predicate.PredicateUtil.BuiltinPredicate;
import suite.node.Node;
import suite.node.Tree;
import suite.node.io.Formatter;
import suite.node.io.TermOp;
import suite.node.pp.PrettyPrinter;

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
		RuleSet ruleSet = prover.ruleSet();
		List<Rule> rules = ruleSet.getRules();
		List<Node> nodes = new ArrayList<>();

		for (Rule rule : rules)
			nodes.add(Tree.of(TermOp.IS____, rule.head, rule.tail));

		return prover.bind(Tree.of(TermOp.NEXT__, nodes), p0);
	});

	public BuiltinPredicate importPredicate = PredicateUtil.p1((prover, p0) -> Suite.importFrom(prover.ruleSet(), p0));

	public BuiltinPredicate importUrl = PredicateUtil.p1((prover, p0) -> {
		String url = Formatter.display(p0);
		try {
			return Suite.importUrl(prover.ruleSet(), url);
		} catch (Exception ex) {
			throw new RuntimeException("exception when importing " + url, ex);
		}
	});

	public BuiltinPredicate list = PredicateUtil.ps((prover, ps) -> {
		Prototype proto = null;
		if (0 < ps.length)
			proto = Prototype.of(ps[0]);

		Node node = Suite.listRules(prover.ruleSet(), proto);
		PrettyPrinter printer = new PrettyPrinter();
		System.out.println(printer.prettyPrint(node));
		return true;
	});

	public BuiltinPredicate retract = PredicateUtil.p1((prover, p0) -> {
		prover.ruleSet().removeRule(Rule.of(p0));
		return true;
	});

	public BuiltinPredicate retractAll = PredicateUtil.p1((prover, p0) -> {
		Rule rule0 = Rule.of(p0);

		RuleSet ruleSet = prover.ruleSet();
		Trail trail = prover.getTrail();
		int pit = trail.getPointInTime();
		List<Rule> targets = new ArrayList<>();

		for (Rule rule : ruleSet.getRules()) {
			if (Binder.bind(rule0.head, rule.head, trail) //
					&& Binder.bind(rule0.tail, rule.tail, trail))
				targets.add(rule);

			trail.unwind(pit);
		}

		for (Rule rule : targets)
			ruleSet.removeRule(rule);

		return true;
	});

	public BuiltinPredicate with = PredicateUtil.p2((prover, p0, p1) -> {
		RuleSet ruleSet = prover.ruleSet();
		RuleSet ruleSet1 = Suite.getRuleSet(p0);
		CompositeRuleSet ruleSet2 = new CompositeRuleSet(ruleSet1, ruleSet);
		return new Prover(ruleSet2).prove(p1);
	});

}
