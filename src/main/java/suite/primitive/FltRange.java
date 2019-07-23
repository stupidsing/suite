package suite.primitive;

import java.util.Comparator;

import suite.object.Object_;
import suite.streamlet.FunUtil.Iterate;

public class FltRange {

	private static FltRange none_ = FltRange.of(FltFunUtil.EMPTYVALUE, FltFunUtil.EMPTYVALUE);

	public float s;
	public float e;

	public static Iterate<FltRange> mapFst(Flt_Flt fun) {
		return pair -> of(fun.apply(pair.s), pair.e);
	}

	public static Iterate<FltRange> mapSnd(Flt_Flt fun) {
		return pair -> of(pair.s, fun.apply(pair.e));
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
		return (pair0, pair1) -> {
			int c = Boolean.compare(pair0 != null, pair1 != null);
			c = c == 0 ? Float.compare(pair0.s, pair1.s) : c;
			c = c == 0 ? Float.compare(pair0.e, pair1.e) : c;
			return c;
		};
	}

	public static Comparator<FltRange> comparatorByFirst() {
		return (pair0, pair1) -> {
			int c = Boolean.compare(pair0 != null, pair1 != null);
			c = c == 0 ? Float.compare(pair0.s, pair1.s) : c;
			return c;
		};
	}

	public static float fst(FltRange pair) {
		return pair.s;
	}

	public static float snd(FltRange pair) {
		return pair.e;
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
		if (Object_.clazz(object) == FltRange.class) {
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
