package suite.lcs;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

import suite.streamlet.FunUtil;
import suite.streamlet.FunUtil.Source;

/**
 * Longest common subsequence using dynamic programming.
 *
 * @author ywsing
 */
public class LcsDp<T> {

	private Node emptyNode = new Node(0, FunUtil.<T> nullSource());
	private Comparator<Node> comparator = (node0, node1) -> node0.length - node1.length;

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
		var dp = (Node[][]) Array.newInstance(Node.class, size0, size1);

		for (var i0 = 0; i0 < size0; i0++)
			for (var i1 = 0; i1 < size1; i1++) {
				var u = 0 < i0 ? dp[i0 - 1][i1] : emptyNode;
				var l = 0 < i1 ? dp[i0][i1 - 1] : emptyNode;
				var lu = 0 < i0 && 0 < i1 ? dp[i0 - 1][i1 - 1] : emptyNode;

				var t0 = l0.get(i0);
				var t1 = l1.get(i1);
				var lu1 = Objects.equals(t0, t1) ? new Node(t0, lu) : lu;

				dp[i0][i1] = Collections.max(List.of(u, l, lu1), comparator);
			}

		var node = dp[size0 - 1][size1 - 1];
		var source = node.source;
		var result = new ArrayList<T>(Collections.nCopies(node.length, null));

		for (var i = node.length - 1; 0 <= i; i--)
			result.set(i, source.source());

		return result;
	}

}
