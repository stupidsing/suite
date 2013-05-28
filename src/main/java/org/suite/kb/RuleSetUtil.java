package org.suite.kb;

import org.suite.doer.Generalizer;
import org.suite.doer.Prover;
import org.suite.doer.TermParser.TermOp;
import org.suite.node.Atom;
import org.suite.node.Node;
import org.suite.node.Tree;

public class RuleSetUtil {

	public static RuleSet create() {
		return new DoubleIndexedRuleSet();
	}

	public static boolean importFrom(RuleSet ruleSet, Node node) {
		Prover prover = new Prover(ruleSet);
		boolean result = true;
		Tree tree;

		while ((tree = Tree.decompose(node, TermOp.NEXT__)) != null) {
			Rule rule = Rule.formRule(tree.getLeft());

			if (rule.getHead() != Atom.NIL)
				ruleSet.addRule(rule);
			else {
				Node goal = new Generalizer().generalize(rule.getTail());
				result &= prover.prove(goal);
			}

			node = tree.getRight();
		}

		return result;
	}

}
