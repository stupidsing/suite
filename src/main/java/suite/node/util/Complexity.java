package suite.node.util;

import static suite.util.Friends.max;

import suite.node.Node;
import suite.node.Tree;

public class Complexity {

	public int complexity(Node node) {
		int max = 0, base = 0;

		while (true) {
			Tree tree = Tree.decompose(node);

			if (tree != null) {
				base++;
				max = max(max, base + complexity(tree.getLeft()));
				node = tree.getRight();
			} else
				return max;
		}
	}

}
