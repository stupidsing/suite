package suite.algo;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.PriorityQueue;

import suite.util.FunUtil.Sink;
import suite.util.FunUtil.Source;
import suite.util.Util;

public class HuffmanTree<Key> {

	private Map<Key, Node> nodesByKey = new HashMap<>();

	private PriorityQueue<Node> priorityQueue = new PriorityQueue<>(0, new Comparator<Node>() {
		public int compare(Node node0, Node node1) {
			return node0.count - node1.count;
		}
	});

	private Node root;

	private class Node {
		private int count;
		private Key key;
		private Node node0, node1;
		private String locator = "";

		private Node(int count, Key key) {
			this.count = count;
			this.key = key;
			nodesByKey.put(key, this);
		}

		private Node(Node node0, Node node1) {
			this.count = node0.count + node1.count;
			this.node0 = node0;
			this.node1 = node1;
		}

	}

	public HuffmanTree(Map<Key, Integer> countsByKey) {
		buildTree(countsByKey);
	}

	public void encodeToBooleans(Sink<Boolean> sink, List<Key> keys) throws IOException {
		for (Key key : keys)
			for (char ch : Util.chars(nodesByKey.get(key).locator))
				sink.sink(ch == '0' ? Boolean.TRUE : Boolean.FALSE);
	}

	public List<Key> decodeFromBooleans(Source<Boolean> source) throws IOException {
		List<Key> keys = new ArrayList<>();
		Boolean b;

		while ((b = source.source()) != null) {
			Node node = root;

			while (node.key == null) {
				node = b ? node.node0 : node.node1;
				b = source.source();
			}

			keys.add(node.key);
		}

		return keys;
	}

	public Map<Key, Integer> getTable() {
		Map<Key, Integer> table = new HashMap<>();

		for (Entry<Key, Node> entry : nodesByKey.entrySet())
			table.put(entry.getKey(), entry.getValue().count);

		return table;
	}

	public void setTable(Map<Key, Integer> table) {
		buildTree(table);
	}

	private void buildTree(Map<Key, Integer> countsByKey) {
		for (Entry<Key, Integer> entry : countsByKey.entrySet())
			priorityQueue.add(new Node(entry.getValue(), entry.getKey()));

		while (priorityQueue.size() > 1) {
			Node node0 = priorityQueue.remove();
			Node node1 = priorityQueue.remove();
			priorityQueue.add(new Node(node0, node1));
		}

		root = !priorityQueue.isEmpty() ? priorityQueue.remove() : null;

		assignLocators("", root);
	}

	private void assignLocators(String locator, Node node) {
		node.locator = locator;

		if (node.node0 != null || node.node1 != null) {
			assignLocators(locator + "0", node.node0);
			assignLocators(locator + "1", node.node1);
		}
	}

}
