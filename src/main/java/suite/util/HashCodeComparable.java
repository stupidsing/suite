package suite.util;

public class HashCodeComparable<E extends HashCodeComparable<?>> implements Comparable<E> {

	@Override
	public int compareTo(E other) {
		return Integer.compare(hashCode(), other.hashCode());
	}

}
