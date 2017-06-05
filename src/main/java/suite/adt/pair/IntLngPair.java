package suite.adt.pair;

import java.util.Comparator;

import suite.primitive.Int_Int;
import suite.primitive.Lng_Lng;
import suite.util.FunUtil.Fun;
import suite.util.Object_;

public class IntLngPair {

	public int t0;
	public long t1;

	public static Fun<IntLngPair, IntLngPair> map0(Int_Int fun) {
		return pair -> of(fun.apply(pair.t0), pair.t1);
	}

	public static Fun<IntLngPair, IntLngPair> map1(Lng_Lng fun) {
		return pair -> of(pair.t0, fun.apply(pair.t1));
	}

	public static IntLngPair of(int t0, long t1) {
		return new IntLngPair(t0, t1);
	}

	private IntLngPair(int t0, long t1) {
		this.t0 = t0;
		this.t1 = t1;
	}

	public static Comparator<IntLngPair> comparator() {
		return (pair0, pair1) -> {
			int c = Boolean.compare(pair0 != null, pair1 != null);
			c = c == 0 ? Integer.compare(pair0.t0, pair1.t0) : c;
			c = c == 0 ? Long.compare(pair0.t1, pair1.t1) : c;
			return c;
		};
	}

	public static Comparator<IntLngPair> comparatorByFirst() {
		return (pair0, pair1) -> {
			int c = Boolean.compare(pair0 != null, pair1 != null);
			c = c == 0 ? Integer.compare(pair0.t0, pair1.t0) : c;
			return c;
		};
	}

	public static int first_(IntLngPair pair) {
		return pair.t0;
	}

	public static long second(IntLngPair pair) {
		return pair.t1;
	}

	@Override
	public boolean equals(Object object) {
		if (Object_.clazz(object) == IntLngPair.class) {
			IntLngPair other = (IntLngPair) object;
			return t0 == other.t0 && t1 == other.t1;
		} else
			return false;
	}

	@Override
	public int hashCode() {
		return Integer.hashCode(t0) + 31 * Long.hashCode(t1);
	}

	@Override
	public String toString() {
		return t0 + ":" + t1;
	}

}
