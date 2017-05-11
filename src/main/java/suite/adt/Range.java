package suite.adt;

import java.util.Objects;

import suite.util.Util;

public class Range<T extends Comparable<? super T>> {

	public final T from;
	public final T to;

	public static <T extends Comparable<? super T>> Range<T> of(T from, T to) {
		return new Range<>(from, to);
	}

	protected Range(T from, T to) {
		this.from = from;
		this.to = to;
	}

	public boolean contains(T t) {
		return from.compareTo(t) <= 0 && t.compareTo(to) < 0;
	}

	public boolean isEmpty() {
		return Util.compare(from, to) < 0;
	}

	@Override
	public boolean equals(Object object) {
		if (Util.clazz(object) == Pair.class) {
			Range<?> other = (Range<?>) object;
			return Objects.equals(from, other.from) && Objects.equals(to, other.to);
		} else
			return false;
	}

	@Override
	public int hashCode() {
		return Objects.hashCode(from) ^ Objects.hashCode(to);
	}

	@Override
	public String toString() {
		return from + "~" + to;
	}

}
