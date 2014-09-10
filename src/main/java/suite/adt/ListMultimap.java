package suite.adt;

import java.util.ArrayList;
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
		this.map = new HashMap<>();
	}

	public boolean containsKey(K key) {
		return map.containsKey(key);
	}

	public Iterable<Entry<K, V>> entries() {
		return () -> new Iterator<Entry<K, V>>() {
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

			public Entry<K, V> next() {
				V v = listIter.next();

				return new Entry<K, V>() {
					private V v_ = v;

					public K getKey() {
						return k;
					}

					public V getValue() {
						return v_;
					}

					public V setValue(V value) {
						V v0 = v_;
						listIter.set(v_ = value);
						return v0;
					}
				};
			}
		};
	}

	public List<V> get(K k) {
		return map.computeIfAbsent(k, k_ -> new ArrayList<>());
	}

	public boolean isEmpty() {
		return map.isEmpty();
	}

	public void put(K k, V v) {
		get(k).add(v);
	}

	public void remove(K k, V v) {
		get(k).remove(v);
	}

}
