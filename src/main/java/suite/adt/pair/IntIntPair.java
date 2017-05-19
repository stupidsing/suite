package suite.adt.pair;

import java.util.Comparator;
import java.util.Objects;

import suite.primitive.IntPrimitiveFun.Int_Int;
import suite.util.FunUtil.Fun;
import suite.util.Object_;

public class IntIntPair {

	public int t0;
	public int t1;

	public static Fun<IntIntPair, IntIntPair> map0(Int_Int fun) {
		return pair -> IntIntPair.of(fun.apply(pair.t0), pair.t1);
	}

	public static Fun<IntIntPair, IntIntPair> map1(Int_Int fun) {
		return pair -> IntIntPair.of(pair.t0, fun.apply(pair.t1));
	}

	public static IntIntPair of(int t0, int t1) {
		return new IntIntPair(t0, t1);
	}

	private IntIntPair(int t0, int t1) {
		this.t0 = t0;
		this.t1 = t1;
	}

	public static Comparator<IntIntPair> comparator() {
		return (pair0, pair1) -> {
			int c = Boolean.compare(pair0 != null, pair1 != null);
			c = c == 0 ? Integer.compare(pair0.t0, pair1.t0) : c;
			c = c == 0 ? Integer.compare(pair0.t1, pair1.t1) : c;
			return c;
		};
	}

	public static Comparator<IntIntPair> comparatorByFirst() {
		return (pair0, pair1) -> {
			int c = Boolean.compare(pair0 != null, pair1 != null);
			c = c == 0 ? Integer.compare(pair0.t0, pair1.t0) : c;
			return c;
		};
	}

	public static int first_(IntIntPair pair) {
		return pair.t0;
	}

	public static int second(IntIntPair pair) {
		return pair.t1;
	}

	@Override
	public boolean equals(Object object) {
		if (Object_.clazz(object) == IntIntPair.class) {
			IntIntPair other = (IntIntPair) object;
			return t0 == other.t0 && t1 == other.t1;
		} else
			return false;
	}

	@Override
	public int hashCode() {
		return Objects.hashCode(t0) ^ Objects.hashCode(t1);
	}

	@Override
	public String toString() {
		return t0 + ":" + t1;
	}

}
