package suite.primitive;

import java.util.Comparator;

import primal.Ob;
import primal.fp.Funs.Iterate;

public class LngRange {

	private static LngRange none_ = LngRange.of(LngFunUtil.EMPTYVALUE, LngFunUtil.EMPTYVALUE);

	public long s;
	public long e;

	public static Iterate<LngRange> mapFst(Lng_Lng fun) {
		return range -> of(fun.apply(range.s), range.e);
	}

	public static Iterate<LngRange> mapSnd(Lng_Lng fun) {
		return range -> of(range.s, fun.apply(range.e));
	}

	public static LngRange none() {
		return none_;
	}

	public static LngRange of(long s, long e) {
		return new LngRange(s, e);
	}

	protected LngRange(long s, long e) {
		update(s, e);
	}

	public static Comparator<LngRange> comparator() {
		return (range0, range1) -> {
			var c = Boolean.compare(range0 != null, range1 != null);
			c = c == 0 ? Long.compare(range0.s, range1.s) : c;
			c = c == 0 ? Long.compare(range0.e, range1.e) : c;
			return c;
		};
	}

	public static Comparator<LngRange> comparatorByFirst() {
		return (range0, range1) -> {
			var c = Boolean.compare(range0 != null, range1 != null);
			c = c == 0 ? Long.compare(range0.s, range1.s) : c;
			return c;
		};
	}

	public static long fst(LngRange range) {
		return range.s;
	}

	public static long snd(LngRange range) {
		return range.e;
	}

	public long length() {
		return (long) (e - s);
	}

	public <O> O map(LngLng_Obj<O> fun) {
		return fun.apply(s, e);
	}

	public void update(long s_, long e_) {
		s = s_;
		e = e_;
	}

	@Override
	public boolean equals(Object object) {
		if (Ob.clazz(object) == LngRange.class) {
			var other = (LngRange) object;
			return s == other.s && e == other.e;
		} else
			return false;
	}

	@Override
	public int hashCode() {
		return Long.hashCode(s) + 31 * Long.hashCode(e);
	}

	@Override
	public String toString() {
		return s + ":" + e;
	}

}
