package suite.primitive.adt.pair;

import java.util.Comparator;

import suite.object.Object_;
import suite.primitive.LngFlt_Obj;
import suite.primitive.LngFunUtil;
import suite.primitive.Lng_Lng;
import suite.primitive.FltFunUtil;
import suite.primitive.Flt_Flt;
import suite.streamlet.FunUtil.Iterate;

public class LngFltPair {

	private static LngFltPair none_ = LngFltPair.of(LngFunUtil.EMPTYVALUE, FltFunUtil.EMPTYVALUE);

	public long t0;
	public float t1;

	public static Iterate<LngFltPair> mapFst(Lng_Lng fun) {
		return pair -> of(fun.apply(pair.t0), pair.t1);
	}

	public static Iterate<LngFltPair> mapSnd(Flt_Flt fun) {
		return pair -> of(pair.t0, fun.apply(pair.t1));
	}

	public static LngFltPair none() {
		return none_;
	}

	public static LngFltPair of(long t0, float t1) {
		return new LngFltPair(t0, t1);
	}

	protected LngFltPair(long t0, float t1) {
		update(t0, t1);
	}

	public static Comparator<LngFltPair> comparator() {
		return (pair0, pair1) -> {
			int c = Boolean.compare(pair0 != null, pair1 != null);
			c = c == 0 ? Long.compare(pair0.t0, pair1.t0) : c;
			c = c == 0 ? Float.compare(pair0.t1, pair1.t1) : c;
			return c;
		};
	}

	public static Comparator<LngFltPair> comparatorByFirst() {
		return (pair0, pair1) -> {
			int c = Boolean.compare(pair0 != null, pair1 != null);
			c = c == 0 ? Long.compare(pair0.t0, pair1.t0) : c;
			return c;
		};
	}

	public static long fst(LngFltPair pair) {
		return pair.t0;
	}

	public static float snd(LngFltPair pair) {
		return pair.t1;
	}

	public <O> O map(LngFlt_Obj<O> fun) {
		return fun.apply(t0, t1);
	}

	public void update(long t0_, float t1_) {
		t0 = t0_;
		t1 = t1_;
	}

	@Override
	public boolean equals(Object object) {
		if (Object_.clazz(object) == LngFltPair.class) {
			var other = (LngFltPair) object;
			return t0 == other.t0 && t1 == other.t1;
		} else
			return false;
	}

	@Override
	public int hashCode() {
		return Long.hashCode(t0) + 31 * Float.hashCode(t1);
	}

	@Override
	public String toString() {
		return t0 + ":" + t1;
	}

}
