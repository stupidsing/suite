package org.suite.kb;

import java.util.ArrayList;
import java.util.List;

import org.suite.doer.Cloner;
import org.suite.node.Node;

public class CompositeRuleSet implements RuleSet {

	private RuleSet first;
	private RuleSet second;

	public CompositeRuleSet(RuleSet first, RuleSet second) {
		this.first = first;
		this.second = second;
	}

	@Override
	public void clear() {
		first.clear();
		second.clear();
	}

	@Override
	public void addRule(Rule rule) {
		second.addRule(rule);
	}

	@Override
	public void addRuleToFront(Rule rule) {
		second.addRuleToFront(rule);
	}

	@Override
	public void removeRule(Rule rule) {
		first.removeRule(rule);
		second.removeRule(rule);
	}

	/**
	 * If the override rule set contain rules for specified head, return them.
	 * 
	 * Otherwise return what we got from parent rule set.
	 */
	@Override
	public List<Rule> searchRule(Node head) {
		List<Rule> rules = first.searchRule(head);

		if (!rules.isEmpty()) {

			// Clone all the rules once to clear local variable references
			List<Rule> newRules = new ArrayList<>(rules.size());
			Cloner cloner = new Cloner();
			for (Rule rule : rules)
				newRules.add(cloner.clone(rule));
			rules = newRules;
		} else
			rules = second.searchRule(head);

		return rules;
	}

	/**
	 * Returns everything we have anyway: override rule set and parent rule set.
	 */
	@Override
	public List<Rule> getRules() {
		List<Rule> rules = new ArrayList<>();
		rules.addAll(first.getRules());
		rules.addAll(second.getRules());
		return rules;
	}

}
