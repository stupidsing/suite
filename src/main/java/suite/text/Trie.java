package suite.text;

import java.util.HashMap;
import java.util.Map;

import suite.util.FunUtil.Source;
import suite.util.Util;

public class Trie<V> {

	private char start, end;
	private Node root = new Node();
	private Source<V> source;

	public class Node {
		@SuppressWarnings("unchecked")
		private Node branches[] = (Node[]) new Object[end - start];
		private V value = source.source();

		public V getValue() {
			return value;
		}
	}

	public Trie(char start, char end) {
		this(start, end, new Source<V>() {
			public V source() {
				return null;
			}
		});
	}

	public Trie(char start, char end, Source<V> source) {
		this.start = start;
		this.end = end;
		this.source = source;
	}

	public Map<String, V> getMap() {
		Map<String, V> map = new HashMap<>();
		getMap(root, "", map);
		return map;
	}

	private void getMap(Node node, String s, Map<String, V> map) {
		map.put(s, node.value);

		for (char ch = start; ch < end; ch++) {
			Node child = node.branches[ch - start];
			if (child != null)
				getMap(child, s + ch, map);
		}
	}

	public Node getRoot() {
		return root;
	}

	public V get(String s) {
		return descendReadOnly(root, s).value;
	}

	public void put(String s, V value) {
		Node node = root;
		node = descend(node, s);
		node.value = value;
	}

	public Node descendReadOnly(Node node, String s) {
		for (char ch : Util.getChars(s))
			node = node != null ? getNode(node, ch) : null;
		return node;
	}

	public Node descend(Node node, String s) {
		for (char ch : Util.getChars(s)) {
			Node node1 = getNode(node, ch);
			if (node1 == null)
				putNode(node1, ch, node1 = new Node());
			node = node1;
		}
		return node;
	}

	public Node descend(Node node, char ch) {
		return getNode(node, ch);
	}

	private Node getNode(Node node, char ch) {
		return node.branches[ch - start];
	}

	public void putNode(Node node, char ch, Node child) {
		node.branches[ch - start] = child;
	}

}
