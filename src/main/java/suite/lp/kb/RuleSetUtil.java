package suite.lp.kb;

import suite.lp.doer.Generalizer;
import suite.lp.doer.Prover;
import suite.node.Atom;
import suite.node.Node;
import suite.node.Tree;
import suite.node.io.TermParser.TermOp;

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
