package suite.lp.kb;

import suite.lp.doer.Generalizer;
import suite.lp.doer.Prover;
import suite.lp.doer.TermParser.TermOp;
import suite.lp.node.Atom;
import suite.lp.node.Node;
import suite.lp.node.Tree;

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
