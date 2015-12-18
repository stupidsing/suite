package suite.adt;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Map.Entry;

public class ListMultimap<K, V> {

	private Map<K, List<V>> map;

	public ListMultimap() {
		this(new HashMap<>());
	}

	public ListMultimap(Map<K, List<V>> map) {
		this.map = map;
	}

	public boolean containsKey(K key) {
		return map.containsKey(key);
	}

	public Iterable<Pair<K, V>> entries() {
		return () -> new Iterator<Pair<K, V>>() {
			private Iterator<Entry<K, List<V>>> iter = map.entrySet().iterator();
			private ListIterator<V> listIter = Collections.emptyListIterator();
			private K k;

			public boolean hasNext() {
				boolean result;
				while (!(result = listIter.hasNext()) && iter.hasNext()) {
					Entry<K, List<V>> entry = iter.next();
					k = entry.getKey();
					listIter = entry.getValue().listIterator();
				}
				return result;
			}

			public Pair<K, V> next() {
				return Pair.of(k, listIter.next());
			}
		};
	}

	public List<V> get(K k) {
		return map.computeIfAbsent(k, k_ -> new ArrayList<>());
	}

	public boolean isEmpty() {
		for (List<V> value : map.values())
			if (!value.isEmpty())
				return false;
		return true;
	}

	public Iterable<Pair<K, Collection<V>>> listEntries() {
		return () -> map.entrySet().stream().map(e -> {
			Collection<V> col = e.getValue();
			return Pair.of(e.getKey(), col);
		}).iterator();
	}

	public void put(K k, V v) {
		get(k).add(v);
	}

	public void remove(K k, V v) {
		get(k).remove(v);
	}

	public int size() {
		int size = 0;
		for (List<V> value : map.values())
			size += value.size();
		return size;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("{");
		for (Pair<K, V> pair : entries())
			sb.append(pair.t0 + "=" + pair.t1 + ", ");
		sb.append("}");
		return sb.toString();
	}

}
