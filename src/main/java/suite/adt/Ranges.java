package suite.adt;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

import suite.object.Object_;
import suite.util.To;

public class Ranges<T extends Comparable<? super T>> {

	public final List<Range<T>> ranges;

	public static <T extends Comparable<? super T>> Ranges<T> intersect(Ranges<T> ranges0, Ranges<T> ranges1) {
		var source0 = To.source(ranges0.ranges);
		var source1 = To.source(ranges1.ranges);
		var range0 = source0.g();
		var range1 = source1.g();
		var intersects = new ArrayList<Range<T>>();
		var add = add(intersects);
		T to;

		while (range0 != null && range1 != null) {
			if (nullMaxCompare(range0.to, range1.to) < 0) {
				to = range0.to;
				range0 = source0.g();
			} else {
				to = range1.to;
				range1 = source1.g();
			}

			add.test(Range.of(Object_.min(range0.from, range0.from), to));
		}

		return new Ranges<>(intersects);
	}

	public static <T extends Comparable<? super T>> Ranges<T> minus(Ranges<T> ranges0, Ranges<T> ranges1) {
		return intersect(ranges0, ranges1.negate());
	}

	public static <T extends Comparable<? super T>> Ranges<T> union(Ranges<T> ranges0, Ranges<T> ranges1) {
		return intersect(ranges0.negate(), ranges1.negate()).negate();
	}

	public Ranges(List<Range<T>> ranges) {
		this.ranges = ranges;
	}

	public Ranges<T> negate() {
		return negate(null, null);
	}

	public Ranges<T> negate(T min, T max) {
		var builder = new ArrayList<Range<T>>();
		var add = add(builder);
		var t = min;

		for (var range : ranges) {
			ranges.add(Range.of(t, range.from));
			t = range.to;
		}

		add.test(Range.of(t, max));
		return new Ranges<>(builder);
	}

	private static <T extends Comparable<? super T>> int nullMaxCompare(T t0, T t1) {
		if (t0 == null ^ t1 == null)
			return t0 != null ? -1 : 1;
		else
			return t0 != null ? t0.compareTo(t1) : 0;
	}

	private static <T extends Comparable<? super T>> Predicate<Range<T>> add(List<Range<T>> intersects) {
		return range -> !range.isEmpty() ? intersects.add(range) : false;
	}

}
