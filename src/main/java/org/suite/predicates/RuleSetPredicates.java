package org.suite.predicates;

import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;

import org.suite.SuiteUtil;
import org.suite.doer.Formatter;
import org.suite.doer.PrettyPrinter;
import org.suite.doer.Prover;
import org.suite.doer.TermParser.TermOp;
import org.suite.kb.CompositeRuleSet;
import org.suite.kb.Prototype;
import org.suite.kb.Rule;
import org.suite.kb.RuleSet;
import org.suite.kb.RuleSet.RuleSetUtil;
import org.suite.node.Atom;
import org.suite.node.Node;
import org.suite.node.Tree;
import org.suite.predicates.SystemPredicates.SystemPredicate;

public class RuleSetPredicates {

	public static class Asserta implements SystemPredicate {
		public boolean prove(Prover prover, Node ps) {
			Node params[] = Predicate.getParameters(ps, 1);
			prover.getRuleSet().addRuleToFront(Rule.formRule(params[0]));
			return true;
		}
	}

	public static class Assertz implements SystemPredicate {
		public boolean prove(Prover prover, Node ps) {
			Node params[] = Predicate.getParameters(ps, 1);
			prover.getRuleSet().addRule(Rule.formRule(params[0]));
			return true;
		}
	}

	public static class Clear implements SystemPredicate {
		public boolean prove(Prover prover, Node ps) {
			prover.getRuleSet().clear();
			return true;
		}
	}

	public static class GetAllRules implements SystemPredicate {
		public boolean prove(Prover prover, Node ps) {
			List<Rule> rules = prover.getRuleSet().getRules();
			ListIterator<Rule> iter = rules.listIterator(rules.size());
			Node allRules = Atom.NIL;

			while (iter.hasPrevious()) {
				Rule r = iter.previous();
				Node head = r.getHead(), tail = r.getTail();
				Tree node = Tree.create(TermOp.IS____, head, tail);
				allRules = Tree.create(TermOp.NEXT__, node, allRules);
			}

			return prover.bind(allRules, ps);
		}
	}

	public static class Import implements SystemPredicate {
		public boolean prove(Prover prover, Node ps) {
			return RuleSetUtil.importFrom(prover.getRuleSet(), ps);
		}
	}

	public static class ImportFile implements SystemPredicate {
		public boolean prove(Prover prover, Node ps) {
			String filename = Formatter.display(ps);
			try {
				return SuiteUtil.importFrom(prover.getRuleSet(), filename);
			} catch (Exception ex) {
				throw new RuntimeException( //
						"Exception when importing " + filename, ex);
			}
		}
	}

	public static class ListPredicates implements SystemPredicate {
		public boolean prove(Prover prover, Node ps) {
			Prototype proto = null;
			if (ps != Atom.NIL)
				proto = Prototype.get(ps);

			List<Node> nodes = new ArrayList<>();

			for (Rule rule : prover.getRuleSet().getRules()) {
				Prototype p1 = Prototype.get(rule);
				if (proto == null || proto.equals(p1)) {
					Node clause = Rule.formClause(rule);
					nodes.add(clause);
				}
			}

			Node node = Atom.NIL;
			for (int i = nodes.size() - 1; i >= 0; i--)
				node = Tree.create(TermOp.NEXT__, nodes.get(i), node);

			PrettyPrinter printer = new PrettyPrinter();
			System.out.println(printer.prettyPrint(node));

			return true;
		}
	}

	public static class Retract implements SystemPredicate {
		public boolean prove(Prover prover, Node ps) {
			Node params[] = Predicate.getParameters(ps, 1);
			prover.getRuleSet().removeRule(Rule.formRule(params[0]));
			return true;
		}
	}

	public static class With implements SystemPredicate {
		public boolean prove(Prover prover, Node ps) {
			Node params[] = Predicate.getParameters(ps, 2);
			RuleSet ruleSet = RuleSetUtil.create();
			RuleSetUtil.importFrom(ruleSet, params[0]);
			CompositeRuleSet ruleSet1 = new CompositeRuleSet(ruleSet,
					prover.getRuleSet());
			return new Prover(ruleSet1).prove(params[1]);
		}
	}

}
