package suite.adt.pair;

import java.util.Comparator;

import suite.primitive.ChrPrimitiveFun.Chr_Chr;
import suite.primitive.PrimitiveFun.Float_Float;
import suite.util.FunUtil.Fun;
import suite.util.Object_;

public class ChrFltPair {

	public char t0;
	public float t1;

	public static Fun<ChrFltPair, ChrFltPair> map0(Chr_Chr fun) {
		return pair -> of(fun.apply(pair.t0), pair.t1);
	}

	public static Fun<ChrFltPair, ChrFltPair> map1(Float_Float fun) {
		return pair -> of(pair.t0, fun.apply(pair.t1));
	}

	public static ChrFltPair of(char t0, float t1) {
		return new ChrFltPair(t0, t1);
	}

	private ChrFltPair(char t0, float t1) {
		this.t0 = t0;
		this.t1 = t1;
	}

	public static Comparator<ChrFltPair> comparator() {
		return (pair0, pair1) -> {
			int c = Boolean.compare(pair0 != null, pair1 != null);
			c = c == 0 ? Character.compare(pair0.t0, pair1.t0) : c;
			c = c == 0 ? Float.compare(pair0.t1, pair1.t1) : c;
			return c;
		};
	}

	public static Comparator<ChrFltPair> comparatorByFirst() {
		return (pair0, pair1) -> {
			int c = Boolean.compare(pair0 != null, pair1 != null);
			c = c == 0 ? Character.compare(pair0.t0, pair1.t0) : c;
			return c;
		};
	}

	public static char first_(ChrFltPair pair) {
		return pair.t0;
	}

	public static float second(ChrFltPair pair) {
		return pair.t1;
	}

	@Override
	public boolean equals(Object object) {
		if (Object_.clazz(object) == ChrFltPair.class) {
			ChrFltPair other = (ChrFltPair) object;
			return t0 == other.t0 && t1 == other.t1;
		} else
			return false;
	}

	@Override
	public int hashCode() {
		return Character.hashCode(t0) + 31 * Float.hashCode(t1);
	}

	@Override
	public String toString() {
		return t0 + ":" + t1;
	}

}
