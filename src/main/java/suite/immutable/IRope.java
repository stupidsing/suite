package suite.immutable;

import java.util.Arrays;
import java.util.List;

import suite.util.To;
import suite.util.Util;

public class IRope<T> {

	private static int maxBranchFactor = 64;
	private static int minBranchFactor = maxBranchFactor / 2;

	private int depth;
	private int weight;
	private List<T> ts;
	private List<IRope<T>> nodes;

	public IRope(List<T> ts) {
		this.weight = ts.size();
		this.ts = ts;
	}

	public IRope(int depth, List<IRope<T>> nodes) {
		int weight = 0;
		for (IRope<T> node : nodes)
			weight += node.weight;
		this.depth = depth;
		this.weight = weight;
		this.nodes = nodes;
	}

	public T at(int w) {
		if (0 < depth) {
			int index = 0, index1;
			int aw = 0, aw1;
			while ((index1 = index + 1) < nodes.size() && (aw1 = aw + nodes.get(index).weight) <= w) {
				index = index1;
				aw = aw1;
			}
			return nodes.get(index).at(w - aw);
		} else
			return ts.get(w);
	}

	public IRope<T> left(int w) {
		if (0 < depth) {
			int index = nodes.size(), index1;
			int aw = 0, aw1;
			while (0 <= (index1 = index - 1) && (aw1 = aw + nodes.get(index).weight) <= weight - w) {
				index = index1;
				aw = aw1;
			}
			return meld(normalize(Util.left(nodes, index)), nodes.get(index).left(weight - w - aw));
		} else
			return new IRope<>(Util.left(ts, w));
	}

	public IRope<T> right(int w) {
		if (0 < depth) {
			int index = 0, index1;
			int aw = 0, aw1;
			while ((index1 = index + 1) < nodes.size() && (aw1 = aw + nodes.get(index).weight) <= w) {
				index = index1;
				aw = aw1;
			}
			return meld(nodes.get(index).right(w - aw), normalize(Util.right(nodes, index1)));
		} else
			return new IRope<>(Util.right(ts, w));
	}

	public static <T> IRope<T> meld(IRope<T> rope0, IRope<T> rope1) {
		return normalize(meld0(rope0, rope1));
	}

	private static <T> List<IRope<T>> meld0(IRope<T> rope0, IRope<T> rope1) {
		int depth = Math.max(rope0.depth, rope1.depth);
		List<IRope<T>> nodes;

		if (rope1.depth < rope0.depth)
			nodes = To.list(Util.left(rope0.nodes, -1), meld0(Util.last(rope0.nodes), rope1));
		else if (rope0.depth < rope1.depth)
			nodes = To.list(meld0(rope0, Util.first(rope1.nodes)), Util.right(rope1.nodes, 1));
		else if (0 < depth)
			nodes = To.list(rope0.nodes, rope1.nodes);
		else {
			List<T> ts = To.list(rope0.ts, rope1.ts);
			if (maxBranchFactor <= ts.size()) {
				List<T> left = Util.left(ts, minBranchFactor);
				List<T> right = Util.right(ts, minBranchFactor);
				nodes = Arrays.asList(new IRope<>(left), new IRope<>(right));
			} else
				nodes = Arrays.asList(new IRope<>(ts));
		}

		List<IRope<T>> list;
		int size1 = nodes.size();

		if (maxBranchFactor <= size1) {
			List<IRope<T>> left = Util.left(nodes, minBranchFactor);
			List<IRope<T>> right = Util.right(nodes, minBranchFactor);
			list = Arrays.asList(new IRope<>(depth, left), new IRope<>(depth, right));
		} else
			list = Arrays.asList(new IRope<>(depth, nodes));

		return list;
	}

	private static <T> IRope<T> normalize(List<IRope<T>> nodes) {
		IRope<T> node = nodes.get(0);
		return nodes.size() != 1 ? new IRope<>(node.depth + 1, nodes) : node;
	}

}
