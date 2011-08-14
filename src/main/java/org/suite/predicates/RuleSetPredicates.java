package org.suite.predicates;

import java.util.List;
import java.util.ListIterator;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.suite.SuiteUtil;
import org.suite.doer.Formatter;
import org.suite.doer.Prover;
import org.suite.doer.TermParser.TermOp;
import org.suite.kb.CompositeRuleSearcher;
import org.suite.kb.Prototype;
import org.suite.kb.RuleSet;
import org.suite.kb.RuleSet.Rule;
import org.suite.node.Atom;
import org.suite.node.Node;
import org.suite.node.Tree;
import org.suite.predicates.SystemPredicates.SystemPredicate;

public class RuleSetPredicates {

	public static class Import implements SystemPredicate {
		public boolean prove(Prover prover, Node ps) {
			try {
				String filename = Formatter.display(ps);
				SuiteUtil.importFile(prover.getRuleSet(), filename);
				return true;
			} catch (Exception ex) {
				log.info(this, ex);
				return false;
			}
		}

		private Log log = LogFactory.getLog(getClass());
	}

	public static class Assert implements SystemPredicate {
		public boolean prove(Prover prover, Node ps) {
			Node params[] = Predicate.getParameters(ps, 1);
			prover.getRuleSet().addRule(params[0]);
			return true;
		}
	}

	public static class GetAllRules implements SystemPredicate {
		public boolean prove(Prover prover, Node ps) {
			List<Rule> rules = prover.getRuleSearcher().getRules();
			ListIterator<Rule> iter = rules.listIterator(rules.size());
			Node allRules = Atom.nil;

			while (iter.hasPrevious()) {
				Rule r = iter.previous();
				Tree node = new Tree(TermOp.INDUCE, r.getHead(), r.getTail());
				allRules = new Tree(TermOp.NEXT__, node, allRules);
			}

			return prover.bind(allRules, ps);
		}
	}

	public static class ListPredicates implements SystemPredicate {
		public boolean prove(Prover prover, Node ps) {
			Prototype proto = null;
			if (ps != Atom.nil)
				proto = Prototype.get(ps);

			for (Rule rule : prover.getRuleSet().getRules()) {
				Prototype p1 = Prototype.get(rule);
				if (proto == null || proto.equals(p1)) {
					String s = Formatter.dump(RuleSet.formClause(rule));
					System.out.println(s + " #");
				}
			}

			return true;
		}
	}

	public static class Retract implements SystemPredicate {
		public boolean prove(Prover prover, Node ps) {
			Node params[] = Predicate.getParameters(ps, 1);
			prover.getRuleSet().removeRule(params[0]);
			return true;
		}
	}

	public static class With implements SystemPredicate {
		public boolean prove(Prover prover, Node ps) {
			Node params[] = Predicate.getParameters(ps, 2);
			RuleSet ruleSet = new RuleSet();
			ruleSet.importFrom(params[0]);
			CompositeRuleSearcher ruleSearcher = new CompositeRuleSearcher(
					ruleSet, prover.getRuleSearcher());
			return new Prover(ruleSearcher, prover).prove(params[1]);
		}
	}

}
