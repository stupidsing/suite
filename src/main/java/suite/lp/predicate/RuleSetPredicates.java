package suite.lp.predicate;

import java.util.ArrayList;
import java.util.List;

import suite.Suite;
import suite.lp.Journal;
import suite.lp.doer.Binder;
import suite.lp.doer.Prover;
import suite.lp.kb.CompositeRuleSet;
import suite.lp.kb.Prototype;
import suite.lp.kb.Rule;
import suite.lp.kb.RuleSet;
import suite.lp.predicate.PredicateUtil.BuiltinPredicate;
import suite.node.Atom;
import suite.node.Node;
import suite.node.Tree;
import suite.node.io.Formatter;
import suite.node.io.TermOp;
import suite.node.pp.PrettyPrinter;

public class RuleSetPredicates {

	public BuiltinPredicate asserta = (prover, ps) -> {
		Node params[] = Tree.getParameters(ps, 1);
		prover.ruleSet().addRuleToFront(Rule.formRule(params[0]));
		return true;
	};

	public BuiltinPredicate assertz = (prover, ps) -> {
		Node params[] = Tree.getParameters(ps, 1);
		prover.ruleSet().addRule(Rule.formRule(params[0]));
		return true;
	};

	public BuiltinPredicate getAllRules = (prover, ps) -> {
		RuleSet ruleSet = prover.ruleSet();
		List<Rule> rules = ruleSet.getRules();
		List<Node> nodes = new ArrayList<>();

		for (Rule rule : rules)
			nodes.add(Tree.of(TermOp.IS____, rule.head, rule.tail));

		return prover.bind(Tree.of(TermOp.NEXT__, nodes), ps);
	};

	public BuiltinPredicate importPredicate = (prover, ps) -> Suite.importFrom(prover.ruleSet(), ps);

	public BuiltinPredicate importPath = (prover, ps) -> {
		String filename = Formatter.display(ps);
		try {
			return Suite.importPath(prover.ruleSet(), filename);
		} catch (Exception ex) {
			throw new RuntimeException("Exception when importing " + filename, ex);
		}
	};

	public BuiltinPredicate list = (prover, ps) -> {
		Prototype proto = null;
		if (ps != Atom.NIL)
			proto = Prototype.of(ps);

		Node node = Suite.getRuleList(prover.ruleSet(), proto);
		PrettyPrinter printer = new PrettyPrinter();
		System.out.println(printer.prettyPrint(node));
		return true;
	};

	public BuiltinPredicate retract = (prover, ps) -> {
		Node params[] = Tree.getParameters(ps, 1);
		prover.ruleSet().removeRule(Rule.formRule(params[0]));
		return true;
	};

	public BuiltinPredicate retractAll = (prover, ps) -> {
		Node params[] = Tree.getParameters(ps, 1);
		Rule rule0 = Rule.formRule(params[0]);

		RuleSet ruleSet = prover.ruleSet();
		Journal journal = prover.getJournal();
		int pit = journal.getPointInTime();
		List<Rule> targets = new ArrayList<>();

		for (Rule rule : ruleSet.getRules()) {
			if (Binder.bind(rule0.head, rule.head, journal) //
					&& Binder.bind(rule0.tail, rule.tail, journal))
				targets.add(rule);

			journal.undoBinds(pit);
		}

		for (Rule rule : targets)
			ruleSet.removeRule(rule);

		return true;
	};

	public BuiltinPredicate with = (prover, ps) -> {
		Node params[] = Tree.getParameters(ps, 2);
		RuleSet ruleSet = prover.ruleSet();
		RuleSet ruleSet1 = Suite.nodeToRuleSet(params[0]);
		CompositeRuleSet ruleSet2 = new CompositeRuleSet(ruleSet1, ruleSet);
		return new Prover(ruleSet2).prove(params[1]);
	};

}
