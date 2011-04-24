package org.suite.kb;

import java.util.List;

import org.suite.kb.RuleSet.Rule;
import org.suite.node.Node;

public interface RuleSearcher {

	public List<Rule> getRules(Node head);

	public List<Rule> getRules();

}
