package suite.text;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import suite.util.FunUtil.Source;

public class Indexer {

	private Trie<List<Key>> trie = new Trie<>((char) 32, (char) 128, new Source<List<Key>>() {
		public List<Key> source() {
			return new ArrayList<>();
		}
	});

	public class Key {
		private String id;
		private int offset;

		public Key(String id, int offset) {
			this.id = id;
			this.offset = offset;
		}

		public String getId() {
			return id;
		}

		public int getOffset() {
			return offset;
		}
	}

	public void index(String id, String text) {
		int length = text.length();

		for (int start = 0; start < length; start++) {
			int end = start + 1;
			Trie<List<Key>>.Node node = trie.getRoot(), node1;

			while (end < length && (node1 = trie.descend(node, text.charAt(end))) != null) {
				node = node1;
				end++;
			}

			trie.putNode(node, text.charAt(end), node1 = trie.new Node());
			node1.getValue().add(new Key(id, start));
		}
	}

	public Map<String, List<Key>> getMap() {
		return trie.getMap();
	}

}
