package suite.algo;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import suite.util.FunUtil;
import suite.util.FunUtil.Source;
import suite.util.Util;

public class LongestCommonSubsequence<T> {

	private final Node emptyNode = new Node(0, FunUtil.<T> nullSource());

	private final Comparator<Node> comparator = new Comparator<Node>() {
		public int compare(Node node0, Node node1) {
			return node0.length - node1.length;
		}
	};

	private class Node {
		private int length;
		private Source<T> source;

		private Node(T t, Node node) {
			this(node.length + 1, FunUtil.cons(t, node.source));
		}

		private Node(int length, Source<T> source) {
			this.length = length;
			this.source = source;
		}
	}

	public List<T> lcs(List<T> l0, List<T> l1) {
		int size0 = l0.size(), size1 = l1.size();

		@SuppressWarnings("unchecked")
		Node dp[][] = (Node[][]) new LongestCommonSubsequence<?>.Node[size0][size1];

		for (int i0 = 0; i0 < size0; i0++)
			for (int i1 = 0; i1 < size1; i1++) {
				Node u = i0 > 0 ? dp[i0 - 1][i1] : emptyNode;
				Node l = i1 > 0 ? dp[i0][i1 - 1] : emptyNode;
				Node lu = i0 > 0 && i1 > 0 ? dp[i0 - 1][i1 - 1] : emptyNode;

				T t0 = l0.get(i0);
				T t1 = l1.get(i1);
				Node lu1 = Util.equals(t0, t1) ? new Node(t0, lu) : lu;

				dp[i0][i1] = Collections.max(Arrays.asList(u, l, lu1), comparator);
			}

		Node node = dp[size0 - 1][size1 - 1];
		Source<T> source = node.source;
		List<T> result = new ArrayList<>(Collections.<T> nCopies(node.length, null));

		for (int i = node.length - 1; i >= 0; i--)
			result.set(i, source.source());

		return result;
	}

}
