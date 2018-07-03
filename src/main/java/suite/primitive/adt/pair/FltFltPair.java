package suite.primitive.adt.pair;

import java.util.Comparator;

import suite.object.Object_;
import suite.primitive.FltFlt_Obj;
import suite.primitive.FltFunUtil;
import suite.primitive.Flt_Flt;
import suite.streamlet.FunUtil.Iterate;

public class FltFltPair {

	private static FltFltPair none_ = FltFltPair.of(FltFunUtil.EMPTYVALUE, FltFunUtil.EMPTYVALUE);

	public float t0;
	public float t1;

	public static Iterate<FltFltPair> mapFst(Flt_Flt fun) {
		return pair -> of(fun.apply(pair.t0), pair.t1);
	}

	public static Iterate<FltFltPair> mapSnd(Flt_Flt fun) {
		return pair -> of(pair.t0, fun.apply(pair.t1));
	}

	public static FltFltPair none() {
		return none_;
	}

	public static FltFltPair of(float t0, float t1) {
		return new FltFltPair(t0, t1);
	}

	private FltFltPair(float t0, float t1) {
		update(t0, t1);
	}

	public static Comparator<FltFltPair> comparator() {
		return (pair0, pair1) -> {
			int c = Boolean.compare(pair0 != null, pair1 != null);
			c = c == 0 ? Float.compare(pair0.t0, pair1.t0) : c;
			c = c == 0 ? Float.compare(pair0.t1, pair1.t1) : c;
			return c;
		};
	}

	public static Comparator<FltFltPair> comparatorByFirst() {
		return (pair0, pair1) -> {
			int c = Boolean.compare(pair0 != null, pair1 != null);
			c = c == 0 ? Float.compare(pair0.t0, pair1.t0) : c;
			return c;
		};
	}

	public static float fst(FltFltPair pair) {
		return pair.t0;
	}

	public static float snd(FltFltPair pair) {
		return pair.t1;
	}

	public <O> O map(FltFlt_Obj<O> fun) {
		return fun.apply(t0, t1);
	}

	public void update(float t0_, float t1_) {
		t0 = t0_;
		t1 = t1_;
	}

	@Override
	public boolean equals(Object object) {
		if (Object_.clazz(object) == FltFltPair.class) {
			var other = (FltFltPair) object;
			return t0 == other.t0 && t1 == other.t1;
		} else
			return false;
	}

	@Override
	public int hashCode() {
		return Float.hashCode(t0) + 31 * Float.hashCode(t1);
	}

	@Override
	public String toString() {
		return t0 + ":" + t1;
	}

}
