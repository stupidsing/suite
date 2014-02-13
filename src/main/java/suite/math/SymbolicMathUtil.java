package suite.math;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import suite.Suite;
import suite.lp.doer.Prover;
import suite.node.Node;
import suite.node.Reference;
import suite.node.Tree;
import suite.node.util.Complexity;

public class SymbolicMathUtil {

	private static final Complexity complexity = new Complexity();
	private static final Prover prover = Suite.createProver(Arrays.asList("auto.sl", "math.sl"));

	private static Comparator<Node> comparator = new Comparator<Node>() {
		public int compare(Node n0, Node n1) {
			return complexity.complexity(n0) - complexity.complexity(n1);
		}
	};

	public static Node simplify(Node node) {
		int space = 100;
		int complexity0 = complexity.complexity(node);
		Set<Node> searchedNodes = new HashSet<>();
		List<Node> freshNodes = new ArrayList<>();
		searchedNodes.add(node);
		freshNodes.add(node);

		for (int times = 0; times < 20; times++) {
			Collections.sort(freshNodes, comparator);
			if (freshNodes.size() > space)
				freshNodes = freshNodes.subList(0, space);

			List<Node> freshNodes1 = new ArrayList<>();

			for (Node freshNode : freshNodes)
				for (Node equateNode : findEqualities(freshNode))
					if (!searchedNodes.contains(equateNode) && complexity.complexity(equateNode) < complexity0 + 1) {
						searchedNodes.add(equateNode);
						freshNodes1.add(equateNode);
					}

			freshNodes = freshNodes1;
		}

		return Collections.min(searchedNodes, comparator);
	}

	private static Set<Node> findEqualities(Node node) {
		Node v = new Reference(), r = new Reference();

		Node equate = Suite.substitute("equate1 (.0 = .1)", node, v);
		Node goal = Suite.substitute("find.all .0 .1 .2", v, equate, r);

		if (prover.prove(goal)) {
			Set<Node> results = new HashSet<>();
			r = r.finalNode();

			while (r instanceof Tree) {
				Tree tree = (Tree) r;
				results.add(tree.getLeft());
				r = tree.getRight().finalNode();
			}

			return results;
		} else
			return null;
	}

}
