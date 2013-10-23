package suite.lp.kb;

import suite.lp.doer.Generalizer;
import suite.lp.doer.Prover;
import suite.node.Atom;
import suite.node.Node;
import suite.node.io.TermParser.TermOp;

public class RuleSetUtil {

	public static RuleSet create() {
		return new DoubleIndexedRuleSet();
	}

	public static boolean importFrom(RuleSet ruleSet, Node node) {
		Prover prover = new Prover(ruleSet);
		boolean result = true;

		for (Node elem : Node.iter(TermOp.NEXT__, node)) {
			Rule rule = Rule.formRule(elem);

			if (rule.getHead() != Atom.NIL)
				ruleSet.addRule(rule);
			else {
				Node goal = new Generalizer().generalize(rule.getTail());
				result &= prover.prove(goal);
			}
		}

		return result;
	}

}
