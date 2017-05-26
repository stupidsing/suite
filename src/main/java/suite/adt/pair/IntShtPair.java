package suite.adt.pair;

import java.util.Comparator;

import suite.primitive.Int_Int;
import suite.primitive.Sht_Sht;
import suite.util.FunUtil.Fun;
import suite.util.Object_;

public class IntShtPair {

	public int t0;
	public short t1;

	public static Fun<IntShtPair, IntShtPair> map0(Int_Int fun) {
		return pair -> of(fun.apply(pair.t0), pair.t1);
	}

	public static Fun<IntShtPair, IntShtPair> map1(Sht_Sht fun) {
		return pair -> of(pair.t0, fun.apply(pair.t1));
	}

	public static IntShtPair of(int t0, short t1) {
		return new IntShtPair(t0, t1);
	}

	private IntShtPair(int t0, short t1) {
		this.t0 = t0;
		this.t1 = t1;
	}

	public static Comparator<IntShtPair> comparator() {
		return (pair0, pair1) -> {
			int c = Boolean.compare(pair0 != null, pair1 != null);
			c = c == 0 ? Integer.compare(pair0.t0, pair1.t0) : c;
			c = c == 0 ? Short.compare(pair0.t1, pair1.t1) : c;
			return c;
		};
	}

	public static Comparator<IntShtPair> comparatorByFirst() {
		return (pair0, pair1) -> {
			int c = Boolean.compare(pair0 != null, pair1 != null);
			c = c == 0 ? Integer.compare(pair0.t0, pair1.t0) : c;
			return c;
		};
	}

	public static int first_(IntShtPair pair) {
		return pair.t0;
	}

	public static short second(IntShtPair pair) {
		return pair.t1;
	}

	@Override
	public boolean equals(Object object) {
		if (Object_.clazz(object) == IntShtPair.class) {
			IntShtPair other = (IntShtPair) object;
			return t0 == other.t0 && t1 == other.t1;
		} else
			return false;
	}

	@Override
	public int hashCode() {
		return Integer.hashCode(t0) + 31 * Short.hashCode(t1);
	}

	@Override
	public String toString() {
		return t0 + ":" + t1;
	}

}
