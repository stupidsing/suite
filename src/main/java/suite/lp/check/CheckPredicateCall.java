package suite.lp.check;

import java.util.List;
import java.util.Map;

import primal.MoreVerbs.Read;
import primal.os.Log_;
import suite.lp.kb.Prototype;
import suite.lp.kb.Rule;
import suite.node.Node;
import suite.node.util.TreeUtil;

public class CheckPredicateCall {

	private CheckLogicUtil clu = new CheckLogicUtil();
	private Map<Prototype, Integer> nElementsByPrototype;

	public void check(List<Rule> rules) {
		nElementsByPrototype = clu.getNumberOfElements(rules);
		Read.from(rules).concatMap(rule -> clu.scan(rule.tail)).forEach(this::check);
	}

	private void check(Node node) {
		var prototype = Prototype.of(node);
		if (nElementsByPrototype.get(prototype) != null && TreeUtil.nElements(node) < nElementsByPrototype.get(prototype))
			Log_.warn("Not enough number of elements: " + prototype);
	}

}
