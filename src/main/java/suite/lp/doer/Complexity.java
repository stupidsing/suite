package suite.lp.doer;

import suite.lp.node.Node;
import suite.lp.node.Tree;

public class Complexity {

	public int complexity(Node node) {
		int max = 0, base = 0;

		while (true) {
			Tree tree = Tree.decompose(node);

			if (tree != null) {
				base++;
				max = Math.max(max, base + complexity(tree.getLeft()));
				node = tree.getRight();
			} else
				return max;
		}
	}

}
