package suite.primitive.adt.pair;

import java.util.Comparator;

import suite.primitive.LngFunUtil;
import suite.primitive.LngLng_Obj;
import suite.primitive.Lng_Lng;
import suite.util.FunUtil.Iterate;
import suite.util.Object_;

public class LngLngPair {

	private static LngLngPair none_ = LngLngPair.of(LngFunUtil.EMPTYVALUE, LngFunUtil.EMPTYVALUE);

	public long t0;
	public long t1;

	public static Iterate<LngLngPair> map0(Lng_Lng fun) {
		return pair -> of(fun.apply(pair.t0), pair.t1);
	}

	public static Iterate<LngLngPair> map1(Lng_Lng fun) {
		return pair -> of(pair.t0, fun.apply(pair.t1));
	}

	public static LngLngPair none() {
		return none_;
	}

	public static LngLngPair of(long t0, long t1) {
		return new LngLngPair(t0, t1);
	}

	private LngLngPair(long t0, long t1) {
		this.t0 = t0;
		this.t1 = t1;
	}

	public static Comparator<LngLngPair> comparator() {
		return (pair0, pair1) -> {
			int c = Boolean.compare(pair0 != null, pair1 != null);
			c = c == 0 ? Long.compare(pair0.t0, pair1.t0) : c;
			c = c == 0 ? Long.compare(pair0.t1, pair1.t1) : c;
			return c;
		};
	}

	public static Comparator<LngLngPair> comparatorByFirst() {
		return (pair0, pair1) -> {
			int c = Boolean.compare(pair0 != null, pair1 != null);
			c = c == 0 ? Long.compare(pair0.t0, pair1.t0) : c;
			return c;
		};
	}

	public static long first_(LngLngPair pair) {
		return pair.t0;
	}

	public static long second(LngLngPair pair) {
		return pair.t1;
	}

	public <O> O apply(LngLng_Obj<O> fun) {
		return fun.apply(t0, t1);
	}

	public void update(long t0_, long t1_) {
		t0 = t0_;
		t1 = t1_;
	}

	@Override
	public boolean equals(Object object) {
		if (Object_.clazz(object) == LngLngPair.class) {
			LngLngPair other = (LngLngPair) object;
			return t0 == other.t0 && t1 == other.t1;
		} else
			return false;
	}

	@Override
	public int hashCode() {
		return Long.hashCode(t0) + 31 * Long.hashCode(t1);
	}

	@Override
	public String toString() {
		return t0 + ":" + t1;
	}

}
