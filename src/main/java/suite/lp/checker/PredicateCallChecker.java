package suite.lp.checker;

import java.util.List;
import java.util.Map;

import suite.lp.kb.Prototype;
import suite.lp.kb.Rule;
import suite.node.Node;
import suite.node.util.TreeUtil;
import suite.os.LogUtil;
import suite.streamlet.Read;

public class PredicateCallChecker {

	private CheckerUtil checkerUtil = new CheckerUtil();
	private Map<Prototype, Integer> nParametersByPrototype;

	public void check(List<Rule> rules) {
		nParametersByPrototype = checkerUtil.getNumberOfParameters(rules);
		Read.from(rules).concatMap(rule -> checkerUtil.scan(rule.tail)).forEach(this::check);
	}

	private void check(Node node) {
		Prototype prototype = Prototype.of(node);
		Integer np0 = nParametersByPrototype.get(prototype);
		int np1 = TreeUtil.getNumberOfParameters(node);
		if (np0 != null && np0 > np1)
			LogUtil.warn("Not enough number of parameters: " + prototype);
	}

}
