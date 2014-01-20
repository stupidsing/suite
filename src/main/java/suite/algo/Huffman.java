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

import suite.util.FunUtil.Sink;
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
		List<Unit> outputUnits = new ArrayList<>();
		final List<Boolean> outputBooleans = new ArrayList<>();

		Sink<Boolean> sink = new Sink<Boolean>() {
			public void sink(Boolean b) {
				outputBooleans.add(b);
			}
		};

		Huffman<Unit> huffman = new Huffman<>(buildHistogram(input));
		huffman.encodeUnits(To.source(input), sink);
		huffman.writeTree(outputUnits);

		return Pair.create(outputUnits, outputBooleans);
	}

	public static <Unit> List<Unit> decode(Pair<List<Unit>, List<Boolean>> input) {
		final List<Unit> output = new ArrayList<>();

		Sink<Unit> sink = new Sink<Unit>() {
			public void sink(Unit unit) {
				output.add(unit);
			}
		};

		new Huffman<>(input.t0).decodeUnits(To.source(input.t1), sink);
		return output;
	}

	private Huffman(List<Unit> units) {
		this.root = readTree(units);
	}

	private Huffman(Map<Unit, Integer> countsByUnit) {
		this.root = buildTree(countsByUnit);
	}

	private void encodeUnits(Source<Unit> source, Sink<Boolean> sink) {
		Unit unit;

		while ((unit = source.source()) != null) {
			Node node = nodesByUnit.get(unit), parent;

			while ((parent = node.parent) != null) {
				sink.sink(parent.node0 == node ? Boolean.FALSE : Boolean.TRUE);
				node = node.parent;
			}
		}
	}

	private void decodeUnits(Source<Boolean> source, Sink<Unit> sink) {
		Boolean b;

		while ((b = source.source()) != null) {
			Node node = root;

			while (node.unit == null) {
				node = b ? node.node0 : node.node1;
				b = source.source();
			}

			sink.sink(node.unit);
		}
	}

	private static <Unit> Map<Unit, Integer> buildHistogram(List<Unit> input) {
		Map<Unit, Integer> countsByUnit = new HashMap<>();

		for (Unit unit : input) {
			Integer count = countsByUnit.get(unit);
			countsByUnit.put(unit, (count != null ? count : 0) + 1);
		}

		return countsByUnit;
	}

	private Node buildTree(Map<Unit, Integer> countsByUnit) {
		PriorityQueue<Node> priorityQueue = new PriorityQueue<>(0, new Comparator<Node>() {
			public int compare(Node node0, Node node1) {
				return node0.count - node1.count;
			}
		});

		for (Entry<Unit, Integer> entry : countsByUnit.entrySet())
			priorityQueue.add(new Node(entry.getValue(), entry.getKey()));

		while (priorityQueue.size() > 1) {
			Node node0 = priorityQueue.remove();
			Node node1 = priorityQueue.remove();
			priorityQueue.add(new Node(node0, node1));
		}

		return !priorityQueue.isEmpty() ? priorityQueue.remove() : null;
	}

	private Node readTree(List<Unit> units) {
		Deque<Node> stack = new ArrayDeque<>();

		for (Unit unit : units)
			if (unit == null) {
				Node node0 = stack.pop();
				Node node1 = stack.pop();
				stack.push(new Node(node0, node1));
			} else
				stack.push(new Node(0, unit));

		return stack.pop();
	}

	private void writeTree(List<Unit> units) {
		writeTree(units, root);
	}

	private void writeTree(List<Unit> units, Node node) {
		if (node.node0 != null || node.node1 != null) {
			writeTree(units, node.node0);
			writeTree(units, node.node1);
			units.add(null);
		} else
			units.add(node.unit);
	}

}
