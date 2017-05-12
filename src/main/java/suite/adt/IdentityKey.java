package suite.adt;

import suite.util.HashCodeComparable;
import suite.util.Object_;

public class IdentityKey<K> extends HashCodeComparable<IdentityKey<K>> {

	public final K key;

	public static <K> IdentityKey<K> of(K key) {
		return new IdentityKey<>(key);
	}

	private IdentityKey(K node) {
		this.key = node;
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
