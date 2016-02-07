package suite.immutable;

import java.util.Arrays;
import java.util.List;

import suite.util.Util;

public class I23Rope {

	private static int maxBranchFactor = 4;
	private static int minBranchFactor = maxBranchFactor / 2;

	private int depth;
	private int weight;
	private List<I23Rope> nodes;

	public I23Rope(List<I23Rope> nodes) {
		int weight = 0;
		for (I23Rope node : nodes)
			weight += node.weight;
		this.depth = 0 < nodes.size() ? nodes.get(0).depth + 1 : 0;
		this.weight = weight;
		this.nodes = nodes;
	}

	public I23Rope left(int w) {
		return left0(weight - w);
	}

	public I23Rope right(int w) {
		return right0(w);
	}

	public static I23Rope merge(I23Rope rope0, I23Rope rope1) {
		return normalize(merge0(rope0, rope1));
	}

	private I23Rope left0(int weight) {
		int index = nodes.size(), index1;
		int aw = 0, aw1;
		while (0 <= (index1 = index - 1) && (aw1 = aw + nodes.get(index).weight) <= weight) {
			index = index1;
			aw = aw1;
		}
		return merge(normalize(Util.left(nodes, index)), nodes.get(index).left0(weight - aw));
	}

	private I23Rope right0(int weight) {
		int index = 0, index1;
		int aw = 0, aw1;
		while ((index1 = index + 1) < nodes.size() && (aw1 = aw + nodes.get(index).weight) <= weight) {
			index = index1;
			aw = aw1;
		}
		return merge(nodes.get(index).right0(weight - aw), normalize(Util.right(nodes, index1)));
	}

	private static I23Rope normalize(List<I23Rope> nodes) {
		return nodes.size() != 1 ? new I23Rope(nodes) : nodes.get(0);
	}

	private static List<I23Rope> merge0(I23Rope rope0, I23Rope rope1) {
		List<I23Rope> nodes;

		if (rope1.depth < rope0.depth)
			nodes = Util.add(Util.left(rope0.nodes, -1), merge0(Util.last(rope0.nodes), rope1));
		else if (rope0.depth < rope1.depth)
			nodes = Util.add(merge0(rope0, Util.first(rope1.nodes)), Util.right(rope1.nodes, 1));
		else
			nodes = Util.add(rope0.nodes, rope1.nodes);

		List<I23Rope> list;
		int size1 = nodes.size();

		if (maxBranchFactor <= size1) {
			List<I23Rope> left = Util.left(nodes, minBranchFactor);
			List<I23Rope> right = Util.right(nodes, minBranchFactor);
			list = Arrays.asList(new I23Rope(left), new I23Rope(right));
		} else
			list = Arrays.asList(new I23Rope(nodes));

		return list;
	}

}
