package suite.adt.pair;

import java.util.Comparator;

import suite.primitive.Flt_Flt;
import suite.primitive.Sht_Sht;
import suite.util.FunUtil.Fun;
import suite.util.Object_;

public class FltShtPair {

	public float t0;
	public short t1;

	public static Fun<FltShtPair, FltShtPair> map0(Flt_Flt fun) {
		return pair -> of(fun.apply(pair.t0), pair.t1);
	}

	public static Fun<FltShtPair, FltShtPair> map1(Sht_Sht fun) {
		return pair -> of(pair.t0, fun.apply(pair.t1));
	}

	public static FltShtPair of(float t0, short t1) {
		return new FltShtPair(t0, t1);
	}

	private FltShtPair(float t0, short t1) {
		this.t0 = t0;
		this.t1 = t1;
	}

	public static Comparator<FltShtPair> comparator() {
		return (pair0, pair1) -> {
			int c = Boolean.compare(pair0 != null, pair1 != null);
			c = c == 0 ? Float.compare(pair0.t0, pair1.t0) : c;
			c = c == 0 ? Short.compare(pair0.t1, pair1.t1) : c;
			return c;
		};
	}

	public static Comparator<FltShtPair> comparatorByFirst() {
		return (pair0, pair1) -> {
			int c = Boolean.compare(pair0 != null, pair1 != null);
			c = c == 0 ? Float.compare(pair0.t0, pair1.t0) : c;
			return c;
		};
	}

	public static float first_(FltShtPair pair) {
		return pair.t0;
	}

	public static short second(FltShtPair pair) {
		return pair.t1;
	}

	@Override
	public boolean equals(Object object) {
		if (Object_.clazz(object) == FltShtPair.class) {
			FltShtPair other = (FltShtPair) object;
			return t0 == other.t0 && t1 == other.t1;
		} else
			return false;
	}

	@Override
	public int hashCode() {
		return Float.hashCode(t0) + 31 * Short.hashCode(t1);
	}

	@Override
	public String toString() {
		return t0 + ":" + t1;
	}

}
