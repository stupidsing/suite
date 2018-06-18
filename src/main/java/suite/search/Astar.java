package suite.search;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Set;

import suite.primitive.IntPrimitives.Obj_Int;
import suite.util.FunUtil.Fun;
import suite.util.FunUtil.Source;

public class Astar<Node> {

	private Comparator<NodeInfo> comparator = (ni0, ni1) -> ni0.estimatedCost - ni1.estimatedCost;

	private Fun<Node, Source<Node>> generate;
	private Obj_Int<Node> estimate;

	private PriorityQueue<NodeInfo> open = new PriorityQueue<>(256, comparator);
	private Set<Node> closed = new HashSet<>();

	private class NodeInfo {
		private NodeInfo previous;
		private Node node;
		private int sunkCost;
		private int estimatedCost;

		public NodeInfo(NodeInfo previous, Node node, int sunkCost, int estimatedCost) {
			this.previous = previous;
			this.node = node;
			this.sunkCost = sunkCost;
			this.estimatedCost = estimatedCost;
		}
	}

	public Astar(Fun<Node, Source<Node>> generate, Obj_Int<Node> estimate) {
		this.generate = generate;
		this.estimate = estimate;
	}

	public List<Node> astar(Node start, Node end) {
		open.add(new NodeInfo(null, start, 0, estimate.apply(start)));
		NodeInfo ni;
		Node node1;

		while ((ni = open.remove()) != null) {
			var node = ni.node;

			if (node != end && closed.add(node)) {
				var sunkCost1 = ni.sunkCost + 1;
				var source = generate.apply(node);

				while ((node1 = source.source()) != null)
					open.add(new NodeInfo(ni, node1, sunkCost1, sunkCost1 + estimate.apply(node1)));
			} else {
				var deque = new ArrayDeque<Node>();
				while (ni != null) {
					deque.addFirst(ni.node);
					ni = ni.previous;
				}
				return new ArrayList<>(deque);
			}
		}

		return null;
	}

}
