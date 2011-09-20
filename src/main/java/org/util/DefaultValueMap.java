package org.util;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

public abstract class DefaultValueMap<K, V> implements Map<K, V> {

	private Map<K, V> map;

	protected abstract V getDefaultValue(K key);

	@Override
	public void clear() {
		map.clear();
	}

	@Override
	public boolean containsKey(Object key) {
		return true;
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
	public V get(Object k) {
		V value = map.get(k);
		if (value == null) {
			@SuppressWarnings("unchecked")
			K key = (K) k;
			map.put(key, value = getDefaultValue(key));
		}
		return value;
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
		return map.put(key, value);
	}

	@Override
	public void putAll(Map<? extends K, ? extends V> anotherMap) {
		map.putAll(anotherMap);
	}

	@Override
	public V remove(Object key) {
		return map.remove(key);
	}

	@Override
	public int size() {
		return map.size();
	}

	@Override
	public Collection<V> values() {
		return map.values();
	}

}
