package suite.lp.kb;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import primal.adt.map.ListMultimap;
import suite.node.Node;

/**
 * Index rules by two layers of prototype, which is the leftest element and the
 * leftest of the right element in the rule head or query.
 *
 * @author ywsing
 */
public class DoubleIndexedRuleSet extends IndexedRuleSet {

	// index rules by prototypes.
	// have to use a multi-map implementation that allow null keys.
	private Map<Prototype, ListMultimap<Prototype, Rule>> index0 = new HashMap<>();

	@Override
	public void addRule(Rule rule) {
		super.addRule(rule);
		ruleList(rule).add(rule);
	}

	@Override
	public void addRuleToFront(Rule rule) {
		super.addRuleToFront(rule);
		ruleList(rule).add(0, rule);
	}

	@Override
	public void removeRule(Rule rule) {
		super.removeRule(rule);

		var p0 = Prototype.of(rule);
		var p1 = Prototype.of(rule, 1);
		var index1 = index0.get(p0);

		if (index1 != null) {
			index1.remove(p1, rule);

			if (index1.isEmpty())
				index0.remove(p0);
		}
	}

	@Override
	public List<Rule> searchRule(Node node) {
		var p0 = Prototype.of(node);
		var p1 = Prototype.of(node, 1);

		if (p0 != null && p1 != null && !index0.containsKey(null)) {
			var index1 = index0.get(p0);

			if (index1 != null && !index1.containsKey(null))
				return index1.get(p1);
		}

		return super.searchRule(node);
	}

	private List<Rule> ruleList(Rule rule) {
		var p0 = Prototype.of(rule);
		var p1 = Prototype.of(rule, 1);
		var index1 = index0.computeIfAbsent(p0, any -> new ListMultimap<>());
		return index1.getMutable(p1);
	}

}
