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
import suite.lp.predicate.PredicateUtil.SystemPredicate;
import suite.node.Atom;
import suite.node.Node;
import suite.node.Tree;
import suite.node.io.Formatter;
import suite.node.io.PrettyPrinter;
import suite.node.io.TermOp;

public class RuleSetPredicates {

	public SystemPredicate asserta = (prover, ps) -> {
		Node params[] = Tree.getParameters(ps, 1);
		RuleSet ruleSet = prover.ruleSet();
		ruleSet.addRuleToFront(Rule.formRule(params[0]));
		return true;
	};

	public SystemPredicate assertz = (prover, ps) -> {
		Node params[] = Tree.getParameters(ps, 1);
		Suite.addRule(prover.ruleSet(), params[0]);
		return true;
	};

	public SystemPredicate getAllRules = (prover, ps) -> {
		RuleSet ruleSet = prover.ruleSet();
		List<Rule> rules = ruleSet.getRules();
		List<Node> nodes = new ArrayList<>();

		for (Rule rule : rules)
			nodes.add(Tree.of(TermOp.IS____, rule.getHead(), rule.getTail()));

		return prover.bind(Tree.list(TermOp.NEXT__, nodes), ps);
	};

	public SystemPredicate importPredicate = (prover, ps) -> Suite.importFrom(prover.ruleSet(), ps);

	public SystemPredicate importPath = (prover, ps) -> {
		String filename = Formatter.display(ps);
		try {
			return Suite.importPath(prover.ruleSet(), filename);
		} catch (Exception ex) {
			throw new RuntimeException("Exception when importing " + filename, ex);
		}
	};

	public SystemPredicate list = (prover, ps) -> {
		Prototype proto = null;
		if (ps != Atom.NIL)
			proto = Prototype.of(ps);

		Node node = Suite.getRuleList(prover.ruleSet(), proto);
		PrettyPrinter printer = new PrettyPrinter();
		System.out.println(printer.prettyPrint(node));
		return true;
	};

	public SystemPredicate retract = (prover, ps) -> {
		Node params[] = Tree.getParameters(ps, 1);
		prover.ruleSet().removeRule(Rule.formRule(params[0]));
		return true;
	};

	public SystemPredicate retractAll = (prover, ps) -> {
		Node params[] = Tree.getParameters(ps, 1);
		Rule rule0 = Rule.formRule(params[0]);

		RuleSet ruleSet = prover.ruleSet();
		Journal journal = prover.getJournal();
		int pit = journal.getPointInTime();
		List<Rule> targets = new ArrayList<>();

		for (Rule rule : ruleSet.getRules()) {
			if (Binder.bind(rule0.getHead(), rule.getHead(), journal) //
					&& Binder.bind(rule0.getTail(), rule.getTail(), journal))
				targets.add(rule);

			journal.undoBinds(pit);
		}

		for (Rule rule : targets)
			ruleSet.removeRule(rule);

		return true;
	};

	public SystemPredicate with = (prover, ps) -> {
		Node params[] = Tree.getParameters(ps, 2);
		RuleSet ruleSet = prover.ruleSet();
		RuleSet ruleSet1 = Suite.nodeToRuleSet(params[0]);
		CompositeRuleSet ruleSet2 = new CompositeRuleSet(ruleSet1, ruleSet);
		return new Prover(ruleSet2).prove(params[1]);
	};

}
