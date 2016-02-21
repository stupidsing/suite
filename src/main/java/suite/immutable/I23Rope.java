package suite.immutable;

import java.util.Arrays;
import java.util.List;

import suite.util.Util;

public class I23Rope<T> {

	private static int maxBranchFactor = 64;
	private static int minBranchFactor = maxBranchFactor / 2;

	private int depth;
	private int weight;
	private List<T> ts;
	private List<I23Rope<T>> nodes;

	public I23Rope(List<T> ts) {
		this.weight = ts.size();
		this.ts = ts;
	}

	public I23Rope(int depth, List<I23Rope<T>> nodes) {
		int weight = 0;
		for (I23Rope<T> node : nodes)
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

	public I23Rope<T> left(int w) {
		if (0 < depth) {
			int index = nodes.size(), index1;
			int aw = 0, aw1;
			while (0 <= (index1 = index - 1) && (aw1 = aw + nodes.get(index).weight) <= weight - w) {
				index = index1;
				aw = aw1;
			}
			return merge(normalize(Util.left(nodes, index)), nodes.get(index).left(weight - w - aw));
		} else
			return new I23Rope<>(Util.left(ts, w));
	}

	public I23Rope<T> right(int w) {
		if (0 < depth) {
			int index = 0, index1;
			int aw = 0, aw1;
			while ((index1 = index + 1) < nodes.size() && (aw1 = aw + nodes.get(index).weight) <= w) {
				index = index1;
				aw = aw1;
			}
			return merge(nodes.get(index).right(w - aw), normalize(Util.right(nodes, index1)));
		} else
			return new I23Rope<>(Util.right(ts, w));
	}

	public static <T> I23Rope<T> merge(I23Rope<T> rope0, I23Rope<T> rope1) {
		return normalize(merge0(rope0, rope1));
	}

	private static <T> List<I23Rope<T>> merge0(I23Rope<T> rope0, I23Rope<T> rope1) {
		int depth = Math.max(rope0.depth, rope1.depth);
		List<I23Rope<T>> nodes;

		if (rope1.depth < rope0.depth)
			nodes = Util.add(Util.left(rope0.nodes, -1), merge0(Util.last(rope0.nodes), rope1));
		else if (rope0.depth < rope1.depth)
			nodes = Util.add(merge0(rope0, Util.first(rope1.nodes)), Util.right(rope1.nodes, 1));
		else if (0 < depth)
			nodes = Util.add(rope0.nodes, rope1.nodes);
		else {
			List<T> ts = Util.add(rope0.ts, rope1.ts);
			if (maxBranchFactor <= ts.size()) {
				List<T> left = Util.left(ts, minBranchFactor);
				List<T> right = Util.right(ts, minBranchFactor);
				nodes = Arrays.asList(new I23Rope<>(left), new I23Rope<>(right));
			} else
				nodes = Arrays.asList(new I23Rope<>(ts));
		}

		List<I23Rope<T>> list;
		int size1 = nodes.size();

		if (maxBranchFactor <= size1) {
			List<I23Rope<T>> left = Util.left(nodes, minBranchFactor);
			List<I23Rope<T>> right = Util.right(nodes, minBranchFactor);
			list = Arrays.asList(new I23Rope<>(depth, left), new I23Rope<>(depth, right));
		} else
			list = Arrays.asList(new I23Rope<>(depth, nodes));

		return list;
	}

	private static <T> I23Rope<T> normalize(List<I23Rope<T>> nodes) {
		I23Rope<T> node = nodes.get(0);
		return nodes.size() != 1 ? new I23Rope<>(node.depth + 1, nodes) : node;
	}

}
