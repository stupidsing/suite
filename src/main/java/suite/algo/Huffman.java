package suite.algo;

import static primal.statics.Fail.fail;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import primal.MoreVerbs.Read;
import primal.Verbs.Take;
import primal.adt.Pair;
import primal.fp.Funs.Source;
import primal.primitive.IntMoreVerbs.ReadInt;
import primal.primitive.IntPrim;
import primal.primitive.adt.map.ObjIntMap;
import primal.primitive.adt.pair.IntObjPair;
import primal.primitive.streamlet.IntObjStreamlet;
import primal.puller.Puller;
import suite.adt.PriorityQueue;

/**
 * Huffman compression.
 *
 * @author ywsing
 */
public class Huffman<Unit> {

	public Pair<List<IntObjPair<Unit>>, List<Boolean>> encode(List<Unit> input) {
		var dictionary = build(input);
		var source = Take.from(input);
		var stack = new ArrayDeque<Boolean>();

		var puller = Puller.of((Source<Boolean>) () -> {
			Node parent;
			Unit unit;

			while (stack.isEmpty())
				if ((unit = source.g()) != null) {
					var node = dictionary.nodeByUnit.get(unit);
					while ((parent = node.parent) != null) {
						stack.push(parent.node1 == node);
						node = parent;
					}
				} else
					return null;

			return stack.pop();
		});

		return Pair.of(save(dictionary), puller.toList());
	}

	public List<Unit> decode(Pair<List<IntObjPair<Unit>>, List<Boolean>> input) {
		var root = load(input.k).root;
		var source = Take.from(input.v);

		return Puller.of((Source<Unit>) () -> {
			var node = root;
			Unit unit = null;
			Boolean b;

			while ((unit = node.unit) == null)
				if ((b = source.g()) != null)
					node = b ? node.node1 : node.node0;
				else
					break;

			return unit;
		}).toList();
	}

	private Dictionary build(List<Unit> input) {
		var comparator = Comparator.<Node> comparingInt(node -> node.size);

		@SuppressWarnings("unchecked")
		var clazz = (Class<Node>) (Class<?>) Node.class;

		var nodes = histogram(input).map((count, unit) -> new Node(unit, count)).toList();
		var pq = new PriorityQueue<>(clazz, nodes.size(), comparator);

		for (var node : nodes)
			pq.insert(node);

		while (1 < pq.size()) {
			var node0 = pq.extractMin();
			var node1 = pq.extractMin();
			pq.insert(new Node(node0, node1));
		}

		// root and root.unit must not be empty
		var root0 = !pq.isEmpty() ? pq.extractMin() : dummy;
		var root1 = root0.unit == null ? root0 : new Node(dummy, root0);
		return new Dictionary(root1, Read.from(nodes).toMap(node -> node.unit));
	}

	private Dictionary load(List<IntObjPair<Unit>> list) {
		var nodeByUnit = new HashMap<Unit, Node>();
		var deque = new ArrayDeque<Node>();

		for (var pair : list)
			deque.push(pair.map((t, unit) -> {
				if (t == 0)
					return dummy;
				else if (t == 1) {
					var node1 = deque.pop();
					var node0 = deque.pop();
					return new Node(node0, node1);
				} else if (t == 2) {
					var node_ = new Node(unit, 0);
					nodeByUnit.put(unit, node_);
					return node_;
				} else
					return fail();
			}));

		return new Dictionary(deque.pop(), nodeByUnit);
	}

	private List<IntObjPair<Unit>> save(Dictionary dictionary) {
		var list = new ArrayList<IntObjPair<Unit>>();

		new Object() {
			private void save(Node node) {
				if (node == dummy)
					list.add(IntObjPair.of(0, null));
				else if (node.node0 != null && node.node1 != null) {
					save(node.node0);
					save(node.node1);
					list.add(IntObjPair.of(1, null));
				} else
					list.add(IntObjPair.of(2, node.unit));
			}
		}.save(dictionary.root);

		return list;
	}

	private IntObjStreamlet<Unit> histogram(Iterable<Unit> input) {
		var histogram = new ObjIntMap<Unit>();
		for (var unit : input)
			histogram.update(unit, c -> (c != IntPrim.EMPTYVALUE ? c : 0) + 1);
		return ReadInt.from2(histogram);
	}

	private class Dictionary {
		private Node root;
		private Map<Unit, Node> nodeByUnit;

		private Dictionary(Node root, Map<Unit, Node> nodeByUnit) {
			this.root = root;
			this.nodeByUnit = nodeByUnit;
		}
	}

	private Node dummy = new Node(null, 0);

	private class Node {
		private Unit unit;
		private int size;
		private Node parent;
		private Node node0, node1;

		private Node(Unit unit, int size) {
			this.unit = unit;
			this.size = size;
		}

		private Node(Node node0, Node node1) {
			this.size = node0.size + node1.size;
			this.node0 = node0;
			this.node1 = node1;
			node0.parent = node1.parent = this;
		}
	}

}
