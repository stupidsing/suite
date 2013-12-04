package suite.lp.kb;

import java.util.List;

import suite.node.Node;

public interface RuleSet {

	public void addRule(Rule rule);

	public void addRuleToFront(Rule rule);

	public void removeRule(Rule rule);

	public List<Rule> searchRule(Node node);

	public List<Rule> getRules();

}
