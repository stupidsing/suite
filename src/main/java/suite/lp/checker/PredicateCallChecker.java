package suite.lp.checker;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import suite.lp.kb.Prototype;
import suite.lp.kb.Rule;
import suite.node.Node;
import suite.os.LogUtil;
import suite.streamlet.Read;

public class PredicateCallChecker {

	private CheckerUtil checkerUtil = new CheckerUtil();

	private Map<Prototype, Integer> nParametersByPrototype = new HashMap<>();

	public void check(List<Rule> rules) {
		Read.from(rules).map(rule -> rule.head).forEach(this::putHead);
		Read.from(rules).concatMap(rule -> checkerUtil.scan(rule.tail)).forEach(this::putTail);
	}

	private void putHead(Node node) {
		Prototype prototype = Prototype.of(node);
		int np = checkerUtil.getNumberOfParameters(node);
		nParametersByPrototype.compute(prototype, (p, np0) -> np0 != null ? Math.min(np0, np) : np);
	}

	private void putTail(Node node) {
		Prototype prototype = Prototype.of(node);
		Integer np0 = nParametersByPrototype.get(prototype);
		int np1 = checkerUtil.getNumberOfParameters(node);
		if (np0 != null && np0 > np1)
			LogUtil.warn("Not enough number of parameters: " + prototype);
	}

}
