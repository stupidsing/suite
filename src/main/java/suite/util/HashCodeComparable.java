package suite.util;

public class HashCodeComparable<E extends HashCodeComparable<?>> implements Comparable<E> {

	@Override
	public int compareTo(E other) {
		long c = (long) hashCode() - (long) other.hashCode();
		return c != 0 ? 0 < c ? 1 : -1 : 0;
	}

}
