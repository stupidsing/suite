package suite.adt;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import primal.Verbs.Right;

public class Trie<K, V> {

	private Map<K, Trie<K, V>> map = new HashMap<>();
	private V value;

	public void add(List<K> ks, V v) {
		if (!ks.isEmpty())
			map.computeIfAbsent(ks.get(0), ch_ -> new Trie<>()).add(Right.of(ks, 1), v);
		else
			value = v;
	}

	public V get(List<K> ks) {
		if (!ks.isEmpty()) {
			var trie = map.get(ks.get(0));
			return trie != null ? trie.get(Right.of(ks, 1)) : null;
		} else
			return value;
	}

	public Map<K, Trie<K, V>> getMap() {
		return map;
	}

	public V getValue() {
		return value;
	}

}
