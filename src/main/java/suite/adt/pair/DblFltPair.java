package suite.adt.pair;

import java.util.Comparator;

import suite.primitive.Dbl_Dbl;
import suite.primitive.Flt_Flt;
import suite.util.FunUtil.Fun;
import suite.util.Object_;

public class DblFltPair {

	public double t0;
	public float t1;

	public static Fun<DblFltPair, DblFltPair> map0(Dbl_Dbl fun) {
		return pair -> of(fun.apply(pair.t0), pair.t1);
	}

	public static Fun<DblFltPair, DblFltPair> map1(Flt_Flt fun) {
		return pair -> of(pair.t0, fun.apply(pair.t1));
	}

	public static DblFltPair of(double t0, float t1) {
		return new DblFltPair(t0, t1);
	}

	private DblFltPair(double t0, float t1) {
		this.t0 = t0;
		this.t1 = t1;
	}

	public static Comparator<DblFltPair> comparator() {
		return (pair0, pair1) -> {
			int c = Boolean.compare(pair0 != null, pair1 != null);
			c = c == 0 ? Double.compare(pair0.t0, pair1.t0) : c;
			c = c == 0 ? Float.compare(pair0.t1, pair1.t1) : c;
			return c;
		};
	}

	public static Comparator<DblFltPair> comparatorByFirst() {
		return (pair0, pair1) -> {
			int c = Boolean.compare(pair0 != null, pair1 != null);
			c = c == 0 ? Double.compare(pair0.t0, pair1.t0) : c;
			return c;
		};
	}

	public static double first_(DblFltPair pair) {
		return pair.t0;
	}

	public static float second(DblFltPair pair) {
		return pair.t1;
	}

	@Override
	public boolean equals(Object object) {
		if (Object_.clazz(object) == DblFltPair.class) {
			DblFltPair other = (DblFltPair) object;
			return t0 == other.t0 && t1 == other.t1;
		} else
			return false;
	}

	@Override
	public int hashCode() {
		return Double.hashCode(t0) + 31 * Float.hashCode(t1);
	}

	@Override
	public String toString() {
		return t0 + ":" + t1;
	}

}
