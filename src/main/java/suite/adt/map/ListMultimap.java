package suite.adt.map;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import suite.streamlet.Read;
import suite.streamlet.Streamlet2;

public class ListMultimap<K, V> {

	private Map<K, List<V>> map;

	public ListMultimap() {
		this(new HashMap<>());
	}

	public ListMultimap(Map<K, List<V>> map) {
		this.map = map;
	}

	public void clear() {
		map.clear();
	}

	public boolean containsKey(K key) {
		return map.containsKey(key);
	}

	public Streamlet2<K, V> entries() {
		return listEntries().concatMapValue(Read::from);
	}

	public List<V> get(K k) {
		var list = map.get(k);
		return list != null ? list : List.of();
	}

	public List<V> getMutable(K k) {
		return get_(k);
	}

	public boolean isEmpty() {
		for (var value : map.values())
			if (!value.isEmpty())
				return false;
		return true;
	}

	public Set<K> keySet() {
		return map.keySet();
	}

	public Streamlet2<K, List<V>> listEntries() {
		return Read.from2(map);
	}

	public void put(K k, V v) {
		get_(k).add(v);
	}

	public void remove(K k, V v) {
		get_(k).remove(v);
	}

	public int size() {
		var size = 0;
		for (var value : map.values())
			size += value.size();
		return size;
	}

	@Override
	public String toString() {
		var sb = new StringBuilder();
		sb.append("{");
		for (var e : listEntries())
			sb.append(e.t0 + "=" + e.t1 + ", ");
		sb.append("}");
		return sb.toString();
	}

	private List<V> get_(K k) {
		return map.computeIfAbsent(k, k_ -> new ArrayList<>());
	}

}
