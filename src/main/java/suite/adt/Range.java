package suite.adt;

import suite.util.Util;

public class Range<T extends Comparable<? super T>> {

	public final T from;
	public final T to;

	public static <T extends Comparable<? super T>> Range<T> of(T from, T to) {
		return new Range<T>(from, to);
	}

	private Range(T from, T to) {
		this.from = from;
		this.to = to;
	}

	public boolean isEmpty() {
		return Util.compare(from, to) < 0;
	}

}
