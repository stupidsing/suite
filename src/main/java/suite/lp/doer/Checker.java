package suite.lp.doer;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import suite.adt.ListMultimap;
import suite.lp.kb.Prototype;
import suite.lp.kb.Rule;
import suite.lp.sewing.SewingGeneralizer;
import suite.node.Atom;
import suite.node.Node;
import suite.node.Tree;
import suite.node.Tuple;
import suite.util.LogUtil;
import suite.util.Pair;

/**
 * Check logic rules for typical errors.
 */
public class Checker {

	private Map<Atom, Boolean> isSingleton = new HashMap<>();

	public void check(List<Rule> rules) {
		ListMultimap<Prototype, Rule> rulesByPrototype = new ListMultimap<>();

		for (Rule rule : rules)
			rulesByPrototype.put(Prototype.of(rule), rule);

		// Check for singleton variables
		for (Pair<Prototype, Rule> pair : rulesByPrototype.entries()) {
			Prototype prototype = pair.t0;
			Rule rule = pair.t1;

			scan(rule.getHead());
			scan(rule.getTail());

			for (Entry<Atom, Boolean> entry1 : isSingleton.entrySet())
				if (entry1.getValue() == Boolean.TRUE)
					LogUtil.warn("Variable only used once: " + prototype + "/" + entry1.getKey());
		}
	}

	private void scan(Node node) {
		while (true) {
			node = node.finalNode();

			if (node instanceof Atom) {
				Atom atom = (Atom) node;
				String name = atom.getName();

				// Check all variables starting with alphabets; ignore
				// computer-generated code
				if (name.startsWith(SewingGeneralizer.variablePrefix) //
						&& name.length() >= 2 //
						&& Character.isAlphabetic(name.charAt(1))) {
					Boolean value = isSingleton.get(atom);
					if (value == null)
						value = Boolean.TRUE;
					else if (value == Boolean.TRUE)
						value = Boolean.FALSE;
					isSingleton.put(atom, value);
				}
			} else if (node instanceof Tree) {
				Tree tree = (Tree) node;
				scan(tree.getLeft());
				node = tree.getRight();
				continue;
			} else if (node instanceof Tuple) {
				List<Node> nodes = ((Tuple) node).getNodes();
				nodes.forEach(this::scan);
			}

			break;
		}
	}

}
