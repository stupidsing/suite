package suite.lp.kb;

import java.util.ArrayList;
import java.util.List;

import primal.Verbs.Equals;
import suite.lp.doer.Cloner;
import suite.node.Node;

public class LinearRuleSet implements RuleSet {

	private List<Rule> rules = new ArrayList<>();

	@Override
	public void addRule(Rule rule) {
		rule = new Cloner().clone(rule);
		rules.add(rule);
	}

	@Override
	public void addRuleToFront(Rule rule) {
		rule = new Cloner().clone(rule);
		rules.add(0, rule);
	}

	@Override
	public void removeRule(Rule rule) {
		rules.remove(rule);
	}

	@Override
	public List<Rule> searchRule(Node node) {
		return rules;
	}

	@Override
	public List<Rule> getRules() {
		return rules;
	}

	@Override
	public boolean equals(Object object) {
		return object instanceof LinearRuleSet lrs && Equals.ab(rules, lrs.rules);
	}

	@Override
	public int hashCode() {
		return rules.hashCode();
	}

}
