package org.suite.kb;

import java.util.ArrayList;
import java.util.List;

import org.suite.doer.Cloner;
import org.suite.kb.RuleSet.Rule;
import org.suite.node.Node;

public class CompositeRuleSearcher implements RuleSearcher {

	private RuleSearcher first;
	private RuleSearcher second;

	public CompositeRuleSearcher(RuleSearcher first, RuleSearcher second) {
		this.first = first;
		this.second = second;
	}

	/**
	 * If the override rule set contain rules for specified head, return them.
	 * 
	 * Otherwise return what we got from parent rule set.
	 */
	@Override
	public List<Rule> getRules(Node head) {
		List<Rule> rules = first.getRules(head);

		if (!rules.isEmpty()) {

			// Clone all the rules once to clear local variable references
			List<Rule> newRules = new ArrayList<>(rules.size());
			Cloner cloner = new Cloner();
			for (Rule rule : rules)
				newRules.add(cloner.clone(rule));
			rules = newRules;
		} else
			rules = second.getRules(head);

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
