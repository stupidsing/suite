package suite.immutable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import suite.util.Util;

public class I23Rope {

	private static I23Rope empty = new I23Rope(new ArrayList<I23Rope>());

	private int depth;
	private int weight;
	private List<I23Rope> nodes;

	public I23Rope(List<I23Rope> nodes) {
		int weight = 0;
		for (I23Rope node : nodes)
			weight += node.weight;
		this.depth = nodes.size() > 0 ? nodes.get(0).depth + 1 : 0;
		this.weight = weight;
		this.nodes = nodes;
	}

	public static I23Rope merge(I23Rope rope0, I23Rope rope1) {
		List<I23Rope> nodes = merge0(rope0, rope1);
		if (nodes.size() == 1)
			return nodes.get(0);
		else
			return new I23Rope(nodes);
	}

	public I23Rope left(int w) {
		return left0(weight - w);
	}

	public I23Rope right(int w) {
		return right0(w);
	}

	private static List<I23Rope> merge0(I23Rope rope0, I23Rope rope1) {
		List<I23Rope> nodes;

		if (rope0.depth > rope1.depth)
			nodes = Util.add(Util.left(rope0.nodes, -1), merge0(Util.last(rope0.nodes), rope1));
		else if (rope0.depth < rope1.depth)
			nodes = Util.add(merge0(rope0, Util.first(rope1.nodes)), Util.right(rope1.nodes, 1));
		else
			nodes = Util.add(rope0.nodes, rope1.nodes);

		List<I23Rope> list;
		int size1 = nodes.size();

		if (size1 > 3) {
			int halfSize = size1 / 2;
			List<I23Rope> left = Util.left(nodes, halfSize);
			List<I23Rope> right = Util.right(nodes, halfSize);
			list = Arrays.asList(new I23Rope(left), new I23Rope(right));
		} else
			list = Arrays.asList(new I23Rope(nodes));

		return list;
	}

	private I23Rope left0(int weight) {
		int index = nodes.size();
		int accumulatedWeight = 0;
		while (accumulatedWeight < weight)
			accumulatedWeight += nodes.get(--index).weight;
		if (index > 0)
			return merge(normalize(Util.left(nodes, index - 1)), nodes.get(index).left0(weight - accumulatedWeight));
		else
			return empty;
	}

	private I23Rope right0(int weight) {
		int index = 0;
		int accumulatedWeight = 0;
		while (accumulatedWeight < weight)
			accumulatedWeight += nodes.get(index++).weight;
		if (index < nodes.size())
			return merge(nodes.get(index).right0(weight - accumulatedWeight), normalize(Util.right(nodes, index + 1)));
		else
			return empty;
	}

	private I23Rope normalize(List<I23Rope> nodes) {
		return nodes.size() == 1 ? new I23Rope(nodes) : nodes.get(0);
	}

}
