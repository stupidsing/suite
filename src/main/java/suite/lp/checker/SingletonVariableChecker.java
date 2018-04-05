package suite.lp.checker;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import suite.adt.map.ListMultimap;
import suite.adt.pair.Pair;
import suite.lp.doer.ProverConstant;
import suite.lp.kb.Prototype;
import suite.lp.kb.Rule;
import suite.node.Atom;
import suite.node.Node;
import suite.node.Tree;
import suite.node.io.Rewrite_.NodeRead;
import suite.os.LogUtil;

public class SingletonVariableChecker {

	public void check(List<Rule> rules) {
		ListMultimap<Prototype, Rule> rulesByPrototype = Prototype.multimap(rules);

		for (Pair<Prototype, Rule> e : rulesByPrototype.entries()) {
			Prototype prototype = e.t0;
			Rule rule = e.t1;

			Scanner scanner = new Scanner();
			scanner.scan(rule.head);
			scanner.scan(rule.tail);
			scanner.warn(prototype);
		}
	}

	private class Scanner {
		private Map<Atom, Boolean> isSingleton = new HashMap<>();

		private void scan(Node node) {
			while (true) {
				if (node instanceof Atom) {
					Atom atom = (Atom) node;
					var name = atom.name;

					// check all variables starting with alphabets; ignore
					// computer-generated code
					if (name.startsWith(ProverConstant.variablePrefix) //
							&& 1 < name.length() //
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
				} else
					NodeRead.of(node).children.forEach(p -> {
						scan(p.t0);
						scan(p.t1);
					});

				break;
			}
		}

		private void warn(Prototype prototype) {
			for (var entry1 : isSingleton.entrySet())
				if (entry1.getValue() == Boolean.TRUE)
					LogUtil.warn("Variable only used once: " + prototype + "/" + entry1.getKey());
		}
	}

}
