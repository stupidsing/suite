package suite.adt.map;

import java.util.List;
import java.util.Map;

import primal.MoreVerbs.Read;
import primal.Verbs.Get;
import primal.adt.map.ListMultimap;
import primal.streamlet.Streamlet2;

public class BiListMultimap<K, V> {

	private ListMultimap<K, V> map = new ListMultimap<>();
	private ListMultimap<V, K> inverseMap = new ListMultimap<>();

	public BiListMultimap() {
		this(new ListMultimap<>(), new ListMultimap<>());
	}

	private BiListMultimap(ListMultimap<K, V> map, ListMultimap<V, K> inverseMap) {
		this.map = map;
		this.inverseMap = inverseMap;
	}

	public void clear() {
		map.clear();
		inverseMap.clear();
	}

	public boolean containsKey(K key) {
		return map.containsKey(key);
	}

	public boolean containsValue(V value) {
		return inverseMap.containsKey(value);
	}

	public Streamlet2<K, V> entries() {
		return Read.from2(map);
	}

	public List<V> get(K key) {
		return map.get(key);
	}

	public BiListMultimap<V, K> inverse() {
		return new BiListMultimap<>(inverseMap, map);
	}

	public boolean isEmpty() {
		return map.isEmpty();
	}

	public void put(K key, V value) {
		map.put(key, value);
		inverseMap.put(value, key);
	}

	public void putAll(Map<? extends K, ? extends V> m) {
		for (var e : m.entrySet())
			put(e.getKey(), e.getValue());
	}

	public void remove(K key, V value) {
		map.remove(key, value);
		inverseMap.remove(value, key);
	}

	public int size() {
		return map.size();
	}

	public boolean equals(Object object) {
		return Get.clazz(object) == BiListMultimap.class ? map.equals(((BiListMultimap<?, ?>) object).map) : false;
	}

	public int hashCode() {
		return map.hashCode();
	}

}
