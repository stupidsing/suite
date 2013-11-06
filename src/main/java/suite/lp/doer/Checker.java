package suite.lp.doer;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import suite.lp.kb.Prototype;
import suite.lp.kb.Rule;
import suite.node.Atom;
import suite.node.Node;
import suite.node.Tree;
import suite.util.LogUtil;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap;

/**
 * Check logic rules for typical errors.
 */
public class Checker {

	public void check(List<Rule> rules) {
		ListMultimap<Prototype, Rule> rulesByPrototype = ArrayListMultimap.create();

		for (Rule rule : rules)
			rulesByPrototype.put(Prototype.get(rule), rule);

		// TODO check for singleton variables
		for (Entry<Prototype, Rule> entry : rulesByPrototype.entries()) {
			Prototype prototype = entry.getKey();
			Rule rule = entry.getValue();

			Map<Atom, Boolean> isSingleton = new HashMap<>();
			scanSingletonVariables(isSingleton, rule.getHead());
			scanSingletonVariables(isSingleton, rule.getTail());

			for (Entry<Atom, Boolean> entry1 : isSingleton.entrySet())
				if (entry1.getValue() == Boolean.TRUE)
					LogUtil.warn("Variable only used once: " + prototype + "/" + entry1.getKey());
		}
	}

	private void scanSingletonVariables(Map<Atom, Boolean> isSingleton, Node node) {
		node = node.finalNode();

		if (node instanceof Tree) {
			Tree tree = (Tree) node;
			scanSingletonVariables(isSingleton, tree.getLeft());
			scanSingletonVariables(isSingleton, tree.getRight());
		} else if (node instanceof Atom) {
			Atom atom = (Atom) node;

			if (atom.getName().startsWith(Generalizer.defaultPrefix)) {
				Boolean value = isSingleton.get(atom);
				if (value == null)
					value = Boolean.TRUE;
				else if (value == true)
					value = Boolean.FALSE;
				isSingleton.put(atom, value);
			}
		}
	}

}
