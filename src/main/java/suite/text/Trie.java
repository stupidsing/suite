package suite.text;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import suite.util.FunUtil.Source;
import suite.util.Util;

public class Trie<V> {

	private char start, end;
	private Node root = new Node();
	private Source<V> source;

	public class Node {
		private List<Node> branches = new ArrayList<>(end - start);
		private V value = source.source();

		public Node() {
			for (int i = start; i < end; i++)
				branches.add(null);
		}

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

	public Node getRoot() {
		return root;
	}

	public Map<String, V> getMap() {
		Map<String, V> map = new HashMap<>();
		getMap(root, "", map);
		return map;
	}

	private void getMap(Node node, String s, Map<String, V> map) {
		map.put(s, node.value);

		for (char ch = start; ch < end; ch++) {
			Node child = descend(node, ch);
			if (child != null)
				getMap(child, s + ch, map);
		}
	}

	public V get(String s) {
		return descend(root, s).value;
	}

	public void put(String s, V value) {
		Node node = root;
		node = descendCreate(node, s);
		node.value = value;
	}

	public Node descend(Node node, String s) {
		for (char ch : Util.getChars(s))
			node = node != null ? descend(node, ch) : null;
		return node;
	}

	public Node descendCreate(Node node, String s) {
		for (char ch : Util.getChars(s)) {
			Node node1 = descend(node, ch);
			if (node1 == null)
				put(node1, ch, node1 = new Node());
			node = node1;
		}
		return node;
	}

	public Node descend(Node node, char ch) {
		return node.branches.get(ch - start);
	}

	public void put(Node node, char ch, Node child) {
		node.branches.set(ch - start, child);
	}

}
