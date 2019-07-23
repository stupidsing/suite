package suite.primitive;

import java.util.Comparator;

import suite.object.Object_;
import suite.streamlet.FunUtil.Iterate;

public class LngRange {

	private static LngRange none_ = LngRange.of(LngFunUtil.EMPTYVALUE, LngFunUtil.EMPTYVALUE);

	public long s;
	public long e;

	public static Iterate<LngRange> mapFst(Lng_Lng fun) {
		return pair -> of(fun.apply(pair.s), pair.e);
	}

	public static Iterate<LngRange> mapSnd(Lng_Lng fun) {
		return pair -> of(pair.s, fun.apply(pair.e));
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
		return (pair0, pair1) -> {
			int c = Boolean.compare(pair0 != null, pair1 != null);
			c = c == 0 ? Long.compare(pair0.s, pair1.s) : c;
			c = c == 0 ? Long.compare(pair0.e, pair1.e) : c;
			return c;
		};
	}

	public static Comparator<LngRange> comparatorByFirst() {
		return (pair0, pair1) -> {
			int c = Boolean.compare(pair0 != null, pair1 != null);
			c = c == 0 ? Long.compare(pair0.s, pair1.s) : c;
			return c;
		};
	}

	public static long fst(LngRange pair) {
		return pair.s;
	}

	public static long snd(LngRange pair) {
		return pair.e;
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
		if (Object_.clazz(object) == LngRange.class) {
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
