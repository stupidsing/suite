package suite.math;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
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
import suite.node.util.TermKey;
import suite.util.FunUtil;
import suite.util.To;

public class SymbolicMathUtil {

	private static Complexity complexity = new Complexity();
	private static Prover prover = Suite.createProver(Arrays.asList("auto.sl", "math.sl"));

	private static Comparator<Node> comparator = (n0, n1) -> complexity.complexity(n0) - complexity.complexity(n1);

	public static Node simplify(Node node0) {
		int space = 100;
		int complexity0 = complexity.complexity(node0);
		Set<TermKey> searchedNodes = new HashSet<>();
		List<Node> freshNodes = new ArrayList<>();
		searchedNodes.add(new TermKey(node0));
		freshNodes.add(node0);

		for (int times = 0; times < 20; times++) {
			Collections.sort(freshNodes, comparator);
			if (freshNodes.size() > space)
				freshNodes = freshNodes.subList(0, space);

			List<Node> freshNodes1 = new ArrayList<>();

			for (Node freshNode : freshNodes)
				for (TermKey term : findEqualities(freshNode)) {
					Node node = term.node;

					if (!searchedNodes.contains(term) && complexity.complexity(node) < complexity0 + 1) {
						searchedNodes.add(term);
						freshNodes1.add(node);
					}
				}

			freshNodes = freshNodes1;
		}

		return Collections.min(To.list(FunUtil.map(k -> k.node, To.source(searchedNodes))), comparator);
	}

	private static Collection<TermKey> findEqualities(Node node) {
		Node v = new Reference(), r = new Reference();

		Node equate = Suite.substitute("equate1 (.0 = .1)", node, v);
		Node goal = Suite.substitute("find.all .0 .1 .2", v, equate, r);

		if (prover.prove(goal)) {
			Set<TermKey> results = new HashSet<>();
			r = r.finalNode();

			while (r instanceof Tree) {
				Tree tree = (Tree) r;
				Node left = tree.getLeft();
				results.add(new TermKey(left));
				r = tree.getRight().finalNode();
			}

			return results;
		} else
			return null;
	}

}
