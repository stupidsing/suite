package suite.primitive;

import java.util.Comparator;

import primal.Ob;
import primal.fp.Funs.Iterate;

public class FltRange {

	private static FltRange none_ = FltRange.of(FltFunUtil.EMPTYVALUE, FltFunUtil.EMPTYVALUE);

	public float s;
	public float e;

	public static Iterate<FltRange> mapFst(Flt_Flt fun) {
		return range -> of(fun.apply(range.s), range.e);
	}

	public static Iterate<FltRange> mapSnd(Flt_Flt fun) {
		return range -> of(range.s, fun.apply(range.e));
	}

	public static FltRange none() {
		return none_;
	}

	public static FltRange of(float s, float e) {
		return new FltRange(s, e);
	}

	protected FltRange(float s, float e) {
		update(s, e);
	}

	public static Comparator<FltRange> comparator() {
		return (range0, range1) -> {
			var c = Boolean.compare(range0 != null, range1 != null);
			c = c == 0 ? Float.compare(range0.s, range1.s) : c;
			c = c == 0 ? Float.compare(range0.e, range1.e) : c;
			return c;
		};
	}

	public static Comparator<FltRange> comparatorByFirst() {
		return (range0, range1) -> {
			var c = Boolean.compare(range0 != null, range1 != null);
			c = c == 0 ? Float.compare(range0.s, range1.s) : c;
			return c;
		};
	}

	public static float fst(FltRange range) {
		return range.s;
	}

	public static float snd(FltRange range) {
		return range.e;
	}

	public float length() {
		return (float) (e - s);
	}

	public <O> O map(FltFlt_Obj<O> fun) {
		return fun.apply(s, e);
	}

	public void update(float s_, float e_) {
		s = s_;
		e = e_;
	}

	@Override
	public boolean equals(Object object) {
		if (Ob.clazz(object) == FltRange.class) {
			var other = (FltRange) object;
			return s == other.s && e == other.e;
		} else
			return false;
	}

	@Override
	public int hashCode() {
		return Float.hashCode(s) + 31 * Float.hashCode(e);
	}

	@Override
	public String toString() {
		return s + ":" + e;
	}

}
