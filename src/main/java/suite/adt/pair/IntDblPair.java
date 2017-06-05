package suite.adt.pair;

import java.util.Comparator;

import suite.primitive.Int_Int;
import suite.primitive.Dbl_Dbl;
import suite.util.FunUtil.Fun;
import suite.util.Object_;

public class IntDblPair {

	public int t0;
	public double t1;

	public static Fun<IntDblPair, IntDblPair> map0(Int_Int fun) {
		return pair -> of(fun.apply(pair.t0), pair.t1);
	}

	public static Fun<IntDblPair, IntDblPair> map1(Dbl_Dbl fun) {
		return pair -> of(pair.t0, fun.apply(pair.t1));
	}

	public static IntDblPair of(int t0, double t1) {
		return new IntDblPair(t0, t1);
	}

	private IntDblPair(int t0, double t1) {
		this.t0 = t0;
		this.t1 = t1;
	}

	public static Comparator<IntDblPair> comparator() {
		return (pair0, pair1) -> {
			int c = Boolean.compare(pair0 != null, pair1 != null);
			c = c == 0 ? Integer.compare(pair0.t0, pair1.t0) : c;
			c = c == 0 ? Double.compare(pair0.t1, pair1.t1) : c;
			return c;
		};
	}

	public static Comparator<IntDblPair> comparatorByFirst() {
		return (pair0, pair1) -> {
			int c = Boolean.compare(pair0 != null, pair1 != null);
			c = c == 0 ? Integer.compare(pair0.t0, pair1.t0) : c;
			return c;
		};
	}

	public static int first_(IntDblPair pair) {
		return pair.t0;
	}

	public static double second(IntDblPair pair) {
		return pair.t1;
	}

	@Override
	public boolean equals(Object object) {
		if (Object_.clazz(object) == IntDblPair.class) {
			IntDblPair other = (IntDblPair) object;
			return t0 == other.t0 && t1 == other.t1;
		} else
			return false;
	}

	@Override
	public int hashCode() {
		return Integer.hashCode(t0) + 31 * Double.hashCode(t1);
	}

	@Override
	public String toString() {
		return t0 + ":" + t1;
	}

}
