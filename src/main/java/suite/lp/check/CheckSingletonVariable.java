package suite.lp.check;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import primal.MoreVerbs.Read;
import primal.os.Log_;
import suite.lp.doer.ProverConstant;
import suite.lp.kb.Prototype;
import suite.lp.kb.Rule;
import suite.node.Atom;
import suite.node.Node;
import suite.node.Tree;
import suite.node.io.Rewrite_.NodeRead;

public class CheckSingletonVariable {

	public void check(List<Rule> rules) {
		var rulesByPrototype = Prototype.multimap(rules);

		for (var e : Read.from2(rulesByPrototype))
			e.map((prototype, rule) -> {
				var scanner = new Scanner();
				scanner.scan(rule.head);
				scanner.scan(rule.tail);
				scanner.warn(prototype);
				return scanner;
			});
	}

	private class Scanner {
		private Map<Node, Boolean> isSingleton = new HashMap<>();

		private void scan(Node node) {
			while (true) {
				if (node instanceof Atom atom) {
					var name = atom.name;

					// check all variables starting with alphabets; ignore
					// computer-generated code
					if (name.startsWith(ProverConstant.variablePrefix) //
							&& 1 < name.length() //
							&& Character.isAlphabetic(name.charAt(1))) {
						var value = isSingleton.get(node);
						if (value == null)
							value = Boolean.TRUE;
						else if (value == Boolean.TRUE)
							value = Boolean.FALSE;
						isSingleton.put(node, value);
					}
				} else if (node instanceof Tree tree) {
					scan(tree.getLeft());
					node = tree.getRight();
					continue;
				} else
					NodeRead.of(node).children.sink((k, v) -> {
						scan(k);
						scan(v);
					});

				break;
			}
		}

		private void warn(Prototype prototype) {
			for (var entry1 : isSingleton.entrySet())
				if (entry1.getValue() == Boolean.TRUE)
					Log_.warn("Variable only used once: " + prototype + "/" + entry1.getKey());
		}
	}

}
