package suite.lp.kb;

import java.util.List;

import suite.lp.node.Node;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap;

/**
 * Index rules by default prototype, which is the leftest element in the rule
 * head or query.
 * 
 * @author ywsing
 */
public class IndexedRuleSet extends LinearRuleSet {

	// Have to use a multi-map implementation that allow null keys
	private ListMultimap<Prototype, Rule> index = ArrayListMultimap.create();

	@Override
	public void clear() {
		super.clear();
		index.clear();
	}

	@Override
	public void addRule(Rule rule) {
		super.addRule(rule);
		index.get(Prototype.get(rule)).add(rule);
	}

	@Override
	public void addRuleToFront(Rule rule) {
		super.addRuleToFront(rule);
		index.get(Prototype.get(rule)).add(0, rule);
	}

	@Override
	public void removeRule(Rule rule) {
		super.removeRule(rule);
		index.remove(Prototype.get(rule), rule);
	}

	@Override
	public List<Rule> searchRule(Node node) {
		Prototype proto = Prototype.get(node);

		// If the query is "un-prototype-able," or the rule set contains
		// "un-prototype-able" entries, full traversal is required
		if (proto != null && !index.containsKey(null))
			return index.get(proto);
		else
			return getRules();
	}

}
