package suite.lp.kb;

import primal.adt.map.ListMultimap;
import suite.node.Node;

import java.util.List;

/**
 * Index rules by default prototype, which is the leftest element in the rule
 * head or query.
 *
 * @author ywsing
 */
public class IndexedRuleSet extends LinearRuleSet {

	// have to use a multi-map implementation that allow null keys
	private ListMultimap<Prototype, Rule> index = new ListMultimap<>();

	@Override
	public void addRule(Rule rule) {
		super.addRule(rule);
		index.getMutable(Prototype.of(rule)).add(rule);
	}

	@Override
	public void addRuleToFront(Rule rule) {
		super.addRuleToFront(rule);
		index.getMutable(Prototype.of(rule)).add(0, rule);
	}

	@Override
	public void removeRule(Rule rule) {
		super.removeRule(rule);
		index.remove(Prototype.of(rule), rule);
	}

	@Override
	public List<Rule> searchRule(Node node) {
		var proto = Prototype.of(node);

		// if the query is "un-prototype-able," or the rule set contains
		// "un-prototype-able" entries, full traversal is required
		if (proto != null && !index.containsKey(null))
			return index.get(proto);
		else
			return getRules();
	}

}
