package org.math;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.suite.Suite;
import org.suite.doer.Prover;
import org.suite.doer.TermParser.TermOp;
import org.suite.node.Node;
import org.suite.node.Tree;

public class MathUtil {

	private static final Prover prover = Suite.createProver(Arrays.asList(
			"auto.sl", "math.sl"));

	private static Comparator<Node> comparator = new Comparator<Node>() {
		public int compare(Node n0, Node n1) {
			return complexity(n0) - complexity(n1);
		}
	};

	public static Node simplify(Node node) {
		int space = 100;
		int complexity0 = complexity(node);
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
				for (Node equateNode : equate(freshNode))
					if (!searchedNodes.contains(equateNode)
							&& complexity(equateNode) < complexity0 + 1) {
						searchedNodes.add(equateNode);
						freshNodes1.add(equateNode);
					}

			freshNodes = freshNodes1;
		}

		return Collections.min(searchedNodes, comparator);
	}

	public static Set<Node> equate(Node node) {
		Node v = Node.ref(), r = Node.ref();

		Node equate = Node.list(Node.atom("equate") //
				, Node.list(TermOp.EQUAL_, node, v));
		Node goal = Node.list(Node.atom("find.all"), v, equate, r);

		System.out.println(goal);

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

	public static int complexity(Node node) {
		if (node instanceof Tree) {
			Tree tree = (Tree) node;
			Node l = tree.getLeft();
			Node r = tree.getRight();
			return Math.max(complexity(l), complexity(r)) + 1;
		} else
			return 0;
	}

}
