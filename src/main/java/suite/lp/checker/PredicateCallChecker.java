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
	private Map<Prototype, Integer> nElementsByPrototype;

	public void check(List<Rule> rules) {
		nElementsByPrototype = checkerUtil.getNumberOfElements(rules);
		Read.from(rules).concatMap(rule -> checkerUtil.scan(rule.tail)).forEach(this::check);
	}

	private void check(Node node) {
		Prototype prototype = Prototype.of(node);
		if (nElementsByPrototype.get(prototype) != null && TreeUtil.getNumberOfElements(node) < nElementsByPrototype.get(prototype))
			LogUtil.warn("Not enough number of elements: " + prototype);
	}

}
