package suite.lcs;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import primal.Verbs.Equals;
import suite.persistent.PerList;

/**
 * Longest common subsequence using dynamic programming.
 *
 * @author ywsing
 */
public class LcsDp<T> {

	private Node emptyNode = new Node(0, PerList.end());
	private Comparator<Node> comparator = Comparator.comparingInt(node -> node.length);

	private class Node {
		private T t;
		private int length;
		private PerList<Node> list;

		private Node(T t, Node node) {
			this.t = t;
			this.length = node.length + 1;
			this.list = PerList.cons(this, node.list);
		}

		private Node(int length, PerList<Node> list) {
			this.length = length;
			this.list = list;
		}
	}

	public List<T> lcs(List<T> l0, List<T> l1) {
		int size0 = l0.size(), size1 = l1.size();

		// var dp = (Node[][]) Array.newInstance(Node.class, size0, size1);
		@SuppressWarnings("unchecked")
		var dp = (Node[][]) Array.newInstance(Node[].class, size0);
		@SuppressWarnings("unchecked")
		var unused0 = (Node[]) Array.newInstance(Node.class, size1);
		@SuppressWarnings("unchecked")
		var unused1 = (Node[]) Array.newInstance(Node.class, size1);

		for (var i0 = 0; i0 < size0; i0++) {
			dp[i0] = unused0;
			unused0 = unused1;
			unused1 = null;

			for (var i1 = 0; i1 < size1; i1++) {
				var u = 0 < i0 ? dp[i0 - 1][i1] : emptyNode;
				var l = 0 < i1 ? dp[i0][i1 - 1] : emptyNode;
				var lu = 0 < i0 && 0 < i1 ? dp[i0 - 1][i1 - 1] : emptyNode;

				var t0 = l0.get(i0);
				var t1 = l1.get(i1);
				var lu1 = Equals.ab(t0, t1) ? new Node(t0, lu) : lu;

				dp[i0][i1] = Collections.max(List.of(u, l, lu1), comparator);
			}

			if (0 < i0) {
				unused1 = unused0;
				unused0 = dp[i0 - 1];
				dp[i0 - 1] = null;
			}
		}

		var node = dp[size0 - 1][size1 - 1];
		var result = new ArrayList<>(Collections.<T> nCopies(node.length, null));
		var i = node.length;

		for (var node_ : node.list)
			result.set(--i, node_.t);

		return result;
	}

}
