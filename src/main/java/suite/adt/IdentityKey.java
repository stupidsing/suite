package suite.adt;

import suite.object.Object_;

public class IdentityKey<K> implements Comparable<IdentityKey<K>> {

	public final K key;

	public static <K> IdentityKey<K> of(K key) {
		return new IdentityKey<>(key);
	}

	private IdentityKey(K key) {
		this.key = key;
	}

	@Override
	public int compareTo(IdentityKey<K> other) {
		return Integer.compare(hashCode(), other.hashCode());
	}

	@Override
	public boolean equals(Object object) {
		return Object_.clazz(object) == IdentityKey.class && key == ((IdentityKey<?>) object).key;
	}

	@Override
	public int hashCode() {
		return System.identityHashCode(key);
	}

}
