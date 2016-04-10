package suite.immutable;

import java.util.Objects;
import java.util.function.BiFunction;

import suite.util.Util;

public class IHashMap<K, V> {

	private IHashSet<Entry<K, V>> set;

	private static class Entry<K, V> {
		private K key;
		private V value;

		private Entry(K key, V value) {
			this.key = key;
			this.value = value;
		}

		public int hashCode() {
			return Objects.hashCode(key);
		}

		public boolean equals(Object object) {
			if (Util.clazz(object) == getClass()) {
				Entry<?, ?> entry = (Entry<?, ?>) object;
				return Objects.equals(key, entry.key);
			} else
				return false;
		}
	}

	public static <K, V> IHashMap<K, V> merge(IHashMap<K, V> map0, IHashMap<K, V> map1, BiFunction<V, V, V> f) {
		return new IHashMap<>(IHashSet.merge(map0.set, map1.set, (e0, e1) -> new Entry<>(e0.key, f.apply(e0.value, e1.value))));
	}

	public IHashMap() {
		this(new IHashSet<>());
	}

	public IHashMap(IHashSet<Entry<K, V>> set) {
		this.set = set;
	}

	public V get(K key) {
		for (Entry<K, V> entry : set.get(key.hashCode()))
			if (Objects.equals(key, entry.key))
				return entry.value;
		return null;
	}

	public IHashMap<K, V> add(K k, V v) {
		return new IHashMap<>(set.add(new Entry<>(k, v)));
	}

	public IHashMap<K, V> remove(K k) {
		return new IHashMap<>(set.remove(new Entry<>(k, null)));
	}

}
