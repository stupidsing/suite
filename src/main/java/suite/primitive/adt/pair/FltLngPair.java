package suite.primitive.adt.pair;

import java.util.Comparator;

import suite.adt.Opt;
import suite.primitive.FltFunUtil;
import suite.primitive.FltLng_Obj;
import suite.primitive.Flt_Flt;
import suite.primitive.LngFunUtil;
import suite.primitive.Lng_Lng;
import suite.util.FunUtil.Iterate;
import suite.util.Object_;

public class FltLngPair {

	private static FltLngPair none_ = FltLngPair.of(FltFunUtil.EMPTYVALUE, LngFunUtil.EMPTYVALUE);

	public float t0;
	public long t1;

	public static Iterate<FltLngPair> map0(Flt_Flt fun) {
		return pair -> of(fun.apply(pair.t0), pair.t1);
	}

	public static Iterate<FltLngPair> map1(Lng_Lng fun) {
		return pair -> of(pair.t0, fun.apply(pair.t1));
	}

	public static FltLngPair none() {
		return none_;
	}

	public static FltLngPair of(float t0, long t1) {
		return new FltLngPair(t0, t1);
	}

	private FltLngPair(float t0, long t1) {
		this.t0 = t0;
		this.t1 = t1;
	}

	public static Comparator<FltLngPair> comparator() {
		return (pair0, pair1) -> {
			int c = Boolean.compare(pair0 != null, pair1 != null);
			c = c == 0 ? Float.compare(pair0.t0, pair1.t0) : c;
			c = c == 0 ? Long.compare(pair0.t1, pair1.t1) : c;
			return c;
		};
	}

	public static Comparator<FltLngPair> comparatorByFirst() {
		return (pair0, pair1) -> {
			int c = Boolean.compare(pair0 != null, pair1 != null);
			c = c == 0 ? Float.compare(pair0.t0, pair1.t0) : c;
			return c;
		};
	}

	public <O> Opt<O> map(FltLng_Obj<O> fun) {
		return t0 != FltFunUtil.EMPTYVALUE ? Opt.of(fun.apply(t0, t1)) : Opt.none();
	}

	public static float first_(FltLngPair pair) {
		return pair.t0;
	}

	public static long second(FltLngPair pair) {
		return pair.t1;
	}

	@Override
	public boolean equals(Object object) {
		if (Object_.clazz(object) == FltLngPair.class) {
			FltLngPair other = (FltLngPair) object;
			return t0 == other.t0 && t1 == other.t1;
		} else
			return false;
	}

	@Override
	public int hashCode() {
		return Float.hashCode(t0) + 31 * Long.hashCode(t1);
	}

	@Override
	public String toString() {
		return t0 + ":" + t1;
	}

}
