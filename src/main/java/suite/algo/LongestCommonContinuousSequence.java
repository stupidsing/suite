package suite.algo;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import suite.net.Bytes;
import suite.primitive.IntList;
import suite.text.Segment;
import suite.util.DefaultFunMap;
import suite.util.FunUtil.Fun;
import suite.util.Pair;

public class LongestCommonContinuousSequence {

	private class Node {
		private IntList starts0 = new IntList();
		private IntList starts1 = new IntList();
	}

	public List<Pair<Segment, Segment>> lccs(Bytes b0, Bytes b1) {
		Node initialNode = new Node();
		int size0 = b0.size();
		int size1 = b1.size();

		for (int i = 0; i < size0; i++)
			initialNode.starts0.add(i);

		for (int i = 0; i < size1; i++)
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
					if (pos0 < size0)
						map.get(b0.byteAt(pos0)).starts0.add(start0);
				}

				for (int start1 : node.starts1) {
					int pos1 = start1 + length;
					if (pos1 < size1)
						map.get(b1.byteAt(pos1)).starts1.add(start1);
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

		List<Pair<Segment, Segment>> results = new ArrayList<>();

		for (Node node : nodes)
			for (int start0 : node.starts0)
				for (int start1 : node.starts1) {
					int end0 = start0 + length, end1 = start1 + length;
					Segment segmentAye = new Segment(start0, end0, new Bytes(b0).subbytes(start0, end0));
					Segment segmentBee = new Segment(start1, end1, new Bytes(b1).subbytes(start0, end1));
					results.add(Pair.create(segmentAye, segmentBee));
				}

		return results;
	}

}
