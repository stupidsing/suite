package suite.algo;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.PriorityQueue;

import suite.util.FunUtil.Source;
import suite.util.Pair;
import suite.util.To;

/**
 * Huffman compression.
 * 
 * @author ywsing
 */
public class Huffman<Unit> {

	private Node root;
	private Map<Unit, Node> nodesByUnit = new HashMap<>();

	private Comparator<Node> comparator = new Comparator<Node>() {
		public int compare(Node node0, Node node1) {
			return node0.count - node1.count;
		}
	};

	private class Node {
		private int count;
		private Unit unit;
		private Node parent;
		private Node node0, node1;

		private Node(int count, Unit unit) {
			this.count = count;
			this.unit = unit;
			nodesByUnit.put(unit, this);
		}

		private Node(Node node0, Node node1) {
			this.count = node0.count + node1.count;
			this.node0 = node0;
			this.node1 = node1;
			node0.parent = node1.parent = this;
		}
	}

	public static <Unit> Pair<List<Unit>, List<Boolean>> encode(List<Unit> input) {
		Huffman<Unit> huffman = new Huffman<>();
		huffman.build(input);

		return Pair.create(huffman.save(), To.list(huffman.encode(To.source(input))));
	}

	public static <Unit> List<Unit> decode(Pair<List<Unit>, List<Boolean>> input) {
		Huffman<Unit> huffman = new Huffman<>();
		huffman.load(input.t0);

		return To.list(huffman.decode(To.source(input.t1)));
	}

	private Huffman() {
	}

	private Source<Boolean> encode(Source<Unit> source) {
		return new Source<Boolean>() {
			private Deque<Boolean> stack = new ArrayDeque<>();

			public Boolean source() {
				Unit unit;

				while (stack.isEmpty() && (unit = source.source()) != null) {
					Node node = nodesByUnit.get(unit), parent;

					while ((parent = node.parent) != null) {
						stack.push(parent.node0 == node ? Boolean.FALSE : Boolean.TRUE);
						node = parent;
					}
				}

				return !stack.isEmpty() ? stack.pop() : null;
			}
		};
	}

	private Source<Unit> decode(Source<Boolean> source) {
		return new Source<Unit>() {
			public Unit source() {
				Boolean b;

				if ((b = source.source()) != null) {
					Node node = root;

					while (node.unit == null) {
						node = b ? node.node0 : node.node1;
						b = source.source();
					}

					return node.unit;
				} else
					return null;
			}
		};
	}

	private void load(List<Unit> units) {
		Deque<Node> deque = new ArrayDeque<>();

		for (Unit unit : units)
			if (unit == null) {
				Node node0 = deque.pop();
				Node node1 = deque.pop();
				deque.push(new Node(node0, node1));
			} else {
				Node node = new Node(0, unit);
				deque.push(node);
				nodesByUnit.put(unit, node);
			}

		root = deque.pop();
	}

	private List<Unit> save() {
		List<Unit> list = new ArrayList<>();
		save(list, root);
		return list;
	}

	private void save(List<Unit> list, Node node) {
		if (node.node0 != null || node.node1 != null) {
			save(list, node.node0);
			save(list, node.node1);
			list.add(null);
		} else
			list.add(node.unit);
	}

	private void build(List<Unit> input) {
		Map<Unit, Integer> histogram = histogram(input);
		PriorityQueue<Node> priorityQueue = new PriorityQueue<>(0, comparator);

		for (Entry<Unit, Integer> entry : histogram.entrySet())
			priorityQueue.add(new Node(entry.getValue(), entry.getKey()));

		while (priorityQueue.size() > 1) {
			Node node0 = priorityQueue.remove();
			Node node1 = priorityQueue.remove();
			priorityQueue.add(new Node(node0, node1));
		}

		root = !priorityQueue.isEmpty() ? priorityQueue.remove() : null;
	}

	private Map<Unit, Integer> histogram(List<Unit> input) {
		Map<Unit, Integer> histogram = new HashMap<>();

		for (Unit unit : input) {
			Integer count = histogram.get(unit);
			histogram.put(unit, (count != null ? count : 0) + 1);
		}

		return histogram;
	}

}
