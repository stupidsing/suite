package suite.algo;

import java.util.HashMap;
import java.util.Map;

// https://stackoverflow.com/questions/9452701/ukkonens-suffix-tree-algorithm-in-plain-english/9513423#9513423
public class SuffixTree {

	public final Node root;

	private int idGenerator;
	private String word;
	private ActivePoint active;
	private int remainder;

	public class Node {
		private int id = idGenerator++;
		private Map<Character, Edge> edges = new HashMap<>();
		private Node link; // suffix link

		public String toString() {
			return "{id:" + id + ",link:" + (link != null ? link.id : null) + ",edges:" + edgesToString() + "}";
		}

		private String edgesToString() {
			var sb = new StringBuilder();
			edges.forEach((k, v) -> sb.append(k + ":" + v + ","));
			return "{" + sb + "}";
		}

		public boolean contains(String suffix) {
			var edge = !suffix.isEmpty() ? edges.get(suffix.charAt(0)) : null;
			return edge != null && edge.contains(suffix);
		}
	}

	private class Edge {
		private int fr;
		private int to;
		private Node next;

		private Edge(int fr, int to, Node next) {
			this.fr = fr;
			this.to = to;
			this.next = next;
		}

		public String toString() {
			return "{'" + word.substring(fr, to) + "'" + (next != null ? ",next:" + next : "") + "}";
		}

		private int getLength() {
			return to - fr;
		}

		private boolean contains(String suffix) {
			if (next != null)
				return suffix.startsWith(word.substring(fr, to)) && next.contains(suffix.substring(to - fr));
			else
				return word.substring(fr, to).equals(suffix);
		}
	}

	private class ActivePoint {
		private Node node;
		private Character activeEdgeChar0;
		private int length;

		private ActivePoint(Node node, Character activeEdgeChar0, int length) {
			this.node = node;
			this.activeEdgeChar0 = activeEdgeChar0;
			this.length = length;
		}

		private ActivePoint moveToLinkOrRoot() {
			return new ActivePoint(node.link != null ? node.link : root, activeEdgeChar0, length);
		}

		private void addEdge(char ch, Edge edge) {
			node.edges.put(ch, edge);
		}

		private void splitEdge(Node node1, String word, int index, char ch) {
			var edge0 = getActiveEdge();
			var edge1 = new Edge(edge0.fr, edge0.fr + length, node1);
			node1.edges.put(word.charAt(edge0.fr + length), new Edge(edge0.fr + length, edge0.to, edge0.next));
			node1.edges.put(ch, new Edge(index, word.length(), null));
			node.edges.put(activeEdgeChar0, edge1);
		}

		private Edge getActiveEdge() {
			return node.edges.get(activeEdgeChar0);
		}
	}

	public SuffixTree(String word) {
		this.word = word;
		root = new Node();
		active = new ActivePoint(root, null, 0);
		remainder = 0;
		build();
	}

	private void build() {
		for (var i = 0; i < word.length(); i++)
			add(i, word.charAt(i));
	}

	private void add(int index, char ch) {
		remainder++;
		var charFound = false;
		Node prevAddedNode = null;
		while (!charFound && 0 < remainder)
			if (active.length <= 0)
				if (active.node.edges.containsKey(ch)) {
					setSuffixLinkTo(prevAddedNode, active.node);
					active = new ActivePoint(active.node, ch, 1);
					moveToNextNodeOfActiveEdgeIfRequired();
					charFound = true;
				} else {
					active.addEdge(ch, new Edge(index, word.length(), null));
					if (active.node == root)
						active = new ActivePoint(root, active.activeEdgeChar0, active.length);
					else {
						var node_ = active.node;
						setSuffixLinkTo(prevAddedNode, node_);
						active = active.moveToLinkOrRoot();
						prevAddedNode = node_;
					}
					remainder--;
				}
			else if (word.charAt(active.getActiveEdge().fr + active.length) == ch) {
				active = new ActivePoint(active.node, active.activeEdgeChar0, active.length + 1);
				moveToNextNodeOfActiveEdgeIfRequired();
				charFound = true;
			} else if (active.node == root)
				prevAddedNode = edgeFromRootNodeHasNotChar(index, ch, prevAddedNode);
			else
				prevAddedNode = edgeFromInternalNodeHasNotChar(index, ch, prevAddedNode);
	}

	private void moveToNextNodeOfActiveEdgeIfRequired() {
		var edge = active.getActiveEdge();
		if (edge.getLength() == active.length)
			active = new ActivePoint(edge.next, null, 0);
	}

	private Node edgeFromRootNodeHasNotChar(int index, char ch, Node prevAddedNode) {
		var node_ = new Node();
		active.splitEdge(node_, word, index, ch);
		setSuffixLinkTo(prevAddedNode, node_);
		active = new ActivePoint(root, word.charAt(index - remainder + 2), active.length - 1);
		walkDown(index);
		return node_;
	}

	private Node edgeFromInternalNodeHasNotChar(int index, char ch, Node prevAddedNode) {
		var node_ = new Node();
		active.splitEdge(node_, word, index, ch);
		setSuffixLinkTo(prevAddedNode, node_);
		active = active.moveToLinkOrRoot();
		walkDown(index);
		return node_;
	}

	private void setSuffixLinkTo(Node source, Node target) {
		if (source != null)
			source.link = target;
	}

	private void walkDown(int index) {
		var activeLength = active.length;
		while (0 < activeLength) {
			var edge = active.getActiveEdge();
			var diff = activeLength - edge.getLength();
			if (0 <= diff)
				active = new ActivePoint(edge.next, 0 < diff ? word.charAt(index - diff) : null, activeLength = diff);
			else
				break;
		}
		remainder--;
	}

	@Override
	public String toString() {
		return root.toString();
	}

}
