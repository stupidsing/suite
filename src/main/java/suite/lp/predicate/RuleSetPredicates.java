package suite.lp.predicate;

import java.util.List;
import java.util.ListIterator;

import suite.Suite;
import suite.lp.doer.Prover;
import suite.lp.kb.CompositeRuleSet;
import suite.lp.kb.Prototype;
import suite.lp.kb.Rule;
import suite.lp.kb.RuleSet;
import suite.lp.kb.RuleSetUtil;
import suite.lp.predicate.SystemPredicates.SystemPredicate;
import suite.node.Atom;
import suite.node.Node;
import suite.node.Tree;
import suite.node.io.Formatter;
import suite.node.io.PrettyPrinter;
import suite.node.io.TermParser.TermOp;

public class RuleSetPredicates {

	public static class Asserta implements SystemPredicate {
		public boolean prove(Prover prover, Node ps) {
			Node params[] = Node.toFixedSizeArray(ps, 1);
			RuleSet ruleSet = prover.ruleSet();
			ruleSet.addRuleToFront(Rule.formRule(params[0]));
			return true;
		}
	}

	public static class Assertz implements SystemPredicate {
		public boolean prove(Prover prover, Node ps) {
			Node params[] = Node.toFixedSizeArray(ps, 1);
			Suite.addRule(prover.ruleSet(), params[0]);
			return true;
		}
	}

	public static class Clear implements SystemPredicate {
		public boolean prove(Prover prover, Node ps) {
			prover.ruleSet().clear();
			return true;
		}
	}

	public static class GetAllRules implements SystemPredicate {
		public boolean prove(Prover prover, Node ps) {
			RuleSet ruleSet = prover.ruleSet();
			List<Rule> rules = ruleSet.getRules();
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
			return RuleSetUtil.importFrom(prover.ruleSet(), ps);
		}
	}

	public static class ImportFile implements SystemPredicate {
		public boolean prove(Prover prover, Node ps) {
			String filename = Formatter.display(ps);
			try {
				return Suite.importFrom(prover.ruleSet(), filename);
			} catch (Exception ex) {
				throw new RuntimeException("Exception when importing " + filename, ex);
			}
		}
	}

	public static class ListPredicates implements SystemPredicate {
		public boolean prove(Prover prover, Node ps) {
			Prototype proto = null;
			if (ps != Atom.NIL)
				proto = Prototype.get(ps);

			Node node = Suite.getRuleList(prover.ruleSet(), proto);
			PrettyPrinter printer = new PrettyPrinter();
			System.out.println(printer.prettyPrint(node));
			return true;
		}
	}

	public static class Retract implements SystemPredicate {
		public boolean prove(Prover prover, Node ps) {
			Node params[] = Node.toFixedSizeArray(ps, 1);
			prover.ruleSet().removeRule(Rule.formRule(params[0]));
			return true;
		}
	}

	public static class With implements SystemPredicate {
		public boolean prove(Prover prover, Node ps) {
			Node params[] = Node.toFixedSizeArray(ps, 2);
			RuleSet ruleSet = prover.ruleSet();
			RuleSet ruleSet1 = Suite.nodeToRuleSet(params[0]);
			CompositeRuleSet ruleSet2 = new CompositeRuleSet(ruleSet1, ruleSet);
			return new Prover(ruleSet2).prove(params[1]);
		}
	}

}
