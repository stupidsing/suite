package suite.java7util;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import suite.util.FunUtil.Fun;

@Deprecated
public class DefaultFunMap<K, V> implements Map<K, V> {

	private Map<K, V> map;
	private Fun<K, V> fun;

	public DefaultFunMap(Fun<K, V> fun) {
		this(Collections.emptyMap(), fun);
	}

	public DefaultFunMap(Map<K, V> map, Fun<K, V> fun) {
		this.map = map;
		this.fun = fun;
	}

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
	public Set<Entry<K, V>> entrySet() {
		return map.entrySet();
	}

	@Override
	public V get(Object k) {
		V value = map.get(k);
		if (value == null) {
			@SuppressWarnings("unchecked")
			K key = (K) k;
			map.put(key, value = fun.apply(key));
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
		if (!Objects.equals(value, fun.apply(key)))
			return map.put(key, value);
		else
			return map.remove(value);
	}

	@Override
	public void putAll(Map<? extends K, ? extends V> anotherMap) {
		anotherMap.entrySet().stream().forEach(entry -> put(entry.getKey(), entry.getValue()));
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
