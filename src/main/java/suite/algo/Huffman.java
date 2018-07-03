package suite.algo;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import suite.adt.PriorityQueue;
import suite.adt.pair.Pair;
import suite.streamlet.FunUtil.Source;
import suite.streamlet.Read;
import suite.util.To;

/**
 * Huffman compression.
 *
 * @author ywsing
 */
public class Huffman {

	public <Unit> Pair<List<Unit>, List<Boolean>> encode(List<Unit> input) {
		var dictionary = build(input);
		return Pair.of(save(dictionary), To.list(encode(dictionary, To.source(input))));
	}

	public <Unit> List<Unit> decode(Pair<List<Unit>, List<Boolean>> input) {
		var dictionary = load(input);
		return To.list(decode(dictionary, To.source(input.t1)));
	}

	private <Unit> Dictionary<Unit> build(List<Unit> input) {
		Comparator<Node<Unit>> comparator = (node0, node1) -> node0.size - node1.size;

		@SuppressWarnings("unchecked")
		var clazz = (Class<Node<Unit>>) (Class<?>) Node.class;

		var nodes = Read //
				.from2(histogram(input)) //
				.map(Node<Unit>::new) //
				.toList();

		var priorityQueue = new PriorityQueue<>(clazz, 0, comparator);

		for (var node : nodes)
			priorityQueue.insert(node);

		while (1 < priorityQueue.size()) {
			var node0 = priorityQueue.extractMin();
			var node1 = priorityQueue.extractMin();
			priorityQueue.insert(new Node<>(node0, node1));
		}

		var dictionary = new Dictionary<Unit>();
		dictionary.root = !priorityQueue.isEmpty() ? priorityQueue.extractMin() : null;
		dictionary.nodeByUnit = Read.from(nodes).toMap(node -> node.unit, node -> node);
		return dictionary;
	}

	private <Unit> Dictionary<Unit> load(Pair<List<Unit>, List<Boolean>> input) {
		var nodeByUnit = new HashMap<Unit, Node<Unit>>();
		var deque = new ArrayDeque<Node<Unit>>();

		for (var unit : input.t0)
			if (unit == null) {
				var node0 = deque.pop();
				var node1 = deque.pop();
				deque.push(new Node<>(node0, node1));
			} else {
				var node = new Node<>(unit, 0);
				deque.push(node);
				nodeByUnit.put(unit, node);
			}

		var dictionary = new Dictionary<Unit>();
		dictionary.root = deque.pop();
		dictionary.nodeByUnit = nodeByUnit;
		return dictionary;
	}

	private <Unit> List<Unit> save(Dictionary<Unit> dictionary) {
		var list = new ArrayList<Unit>();
		save(list, dictionary.root);
		return list;
	}

	private static <Unit> void save(List<Unit> list, Node<Unit> node) {
		if (node.node0 != null || node.node1 != null) {
			save(list, node.node0);
			save(list, node.node1);
			list.add(null);
		} else
			list.add(node.unit);
	}

	private static <Unit> Source<Boolean> encode(Dictionary<Unit> dictionary, Source<Unit> source) {
		var stack = new ArrayDeque<Boolean>();

		return () -> {
			Unit unit;

			while (stack.isEmpty() && (unit = source.source()) != null) {
				var node = dictionary.nodeByUnit.get(unit);
				Node<Unit> parent;

				while ((parent = node.parent) != null) {
					stack.push(parent.node0 == node ? Boolean.FALSE : Boolean.TRUE);
					node = parent;
				}
			}

			return !stack.isEmpty() ? stack.pop() : null;
		};
	}

	private static <Unit> Source<Unit> decode(Dictionary<Unit> dictionary, Source<Boolean> source) {
		return () -> {
			Boolean b;

			if ((b = source.source()) != null) {
				var node = dictionary.root;

				while (node.unit == null) {
					node = b ? node.node0 : node.node1;
					b = source.source();
				}

				return node.unit;
			} else
				return null;
		};
	}

	private static class Dictionary<Unit> {
		private Node<Unit> root;
		private Map<Unit, Node<Unit>> nodeByUnit;
	}

	private static class Node<Unit> {
		private Unit unit;
		private int size;
		private Node<Unit> parent;
		private Node<Unit> node0, node1;

		private Node(Unit unit, int size) {
			this.unit = unit;
			this.size = size;
		}

		private Node(Node<Unit> node0, Node<Unit> node1) {
			this.size = node0.size + node1.size;
			this.node0 = node0;
			this.node1 = node1;
			node0.parent = node1.parent = this;
		}
	}

	private static <Unit> Map<Unit, Integer> histogram(List<Unit> input) {
		var histogram = new HashMap<Unit, Integer>();
		for (var unit : input)
			histogram.put(unit, histogram.getOrDefault(unit, 0) + 1);
		return histogram;
	}

}
