package suite.adt.map;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import suite.object.Object_;

public class HashBiMap<K, V> implements BiMap<K, V> {

	private Map<K, V> map = new HashMap<>();
	private Map<V, K> inverseMap = new HashMap<>();

	public HashBiMap() {
		this(new HashMap<>(), new HashMap<>());
	}

	private HashBiMap(Map<K, V> map, Map<V, K> inverseMap) {
		this.map = map;
		this.inverseMap = inverseMap;
	}

	@Override
	public void clear() {
		map.clear();
		inverseMap.clear();
	}

	@Override
	public boolean containsKey(Object key) {
		return map.containsKey(key);
	}

	@Override
	public boolean containsValue(Object value) {
		return map.containsValue(value);
	}

	@Override
	public Set<java.util.Map.Entry<K, V>> entrySet() {
		return map.entrySet();
	}

	@Override
	public V get(Object key) {
		return map.get(key);
	}

	@Override
	public HashBiMap<V, K> inverse() {
		return new HashBiMap<>(inverseMap, map);
	}

	@Override
	public boolean isEmpty() {
		return map.isEmpty();
	}

	@Override
	public Set<K> keySet() {
		return map.keySet();
	}

	@Override
	public V put(K key, V value) {
		var value0 = map.put(key, value);
		inverseMap.put(value, key);
		return value0;
	}

	@Override
	public void putAll(Map<? extends K, ? extends V> m) {
		for (var e : m.entrySet())
			put(e.getKey(), e.getValue());
	}

	@Override
	public V remove(Object key) {
		var value0 = map.remove(key);
		inverseMap.remove(value0);
		return value0;
	}

	@Override
	public int size() {
		return map.size();
	}

	@Override
	public Collection<V> values() {
		return map.values();
	}

	@Override
	public boolean equals(Object object) {
		return Object_.clazz(object) == HashBiMap.class ? map.equals(((HashBiMap<?, ?>) object).map) : false;
	}

	@Override
	public int hashCode() {
		return map.hashCode();
	}

}
