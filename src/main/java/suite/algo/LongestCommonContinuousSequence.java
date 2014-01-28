package suite.algo;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import suite.primitive.IntList;
import suite.util.DefaultFunMap;
import suite.util.FunUtil.Fun;

public class LongestCommonContinuousSequence {

	private class Node {
		private IntList starts0 = new IntList();
		private IntList starts1 = new IntList();
	}

	public List<IntList> lccs(byte b0[], byte b1[]) {
		Node initialNode = new Node();

		for (int i = 0; i < b0.length; i++)
			initialNode.starts0.add(i);

		for (int i = 0; i < b1.length; i++)
			initialNode.starts1.add(i);

		int length = 0;
		Collection<Node> nodes;
		Collection<Node> nodes1 = Arrays.asList(initialNode);

		while (true) {
			nodes = nodes1;
			nodes1 = new ArrayList<>();

			for (Node node : nodes) {
				Map<Byte, Node> map = new DefaultFunMap<>(new Fun<Byte, Node>() {
					public Node apply(Byte b) {
						return new Node();
					}
				});

				for (int start0 : node.starts0) {
					int pos0 = start0 + length;
					if (pos0 < b0.length)
						map.get(b0[pos0]).starts0.add(start0);
				}

				for (int start1 : node.starts1) {
					int pos1 = start1 + length;
					if (pos1 < b1.length)
						map.get(b1[pos1]).starts1.add(start1);
				}

				for (Node node1 : map.values())
					if (!node1.starts0.isEmpty() && !node1.starts1.isEmpty())
						nodes1.add(node1);
			}

			if (!nodes1.isEmpty())
				length++;
			else
				break;
		}

		List<IntList> results = new ArrayList<>();

		for (Node node : nodes)
			for (int start0 : node.starts0)
				for (int start1 : node.starts1)
					results.add(IntList.asList(start0, start0 + length, start1, start1 + length));

		return results;
	}

}
