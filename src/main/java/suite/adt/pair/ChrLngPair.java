package suite.adt.pair;

import java.util.Comparator;

import suite.primitive.Chr_Chr;
import suite.primitive.Lng_Lng;
import suite.util.FunUtil.Fun;
import suite.util.Object_;

public class ChrLngPair {

	public char t0;
	public long t1;

	public static Fun<ChrLngPair, ChrLngPair> map0(Chr_Chr fun) {
		return pair -> of(fun.apply(pair.t0), pair.t1);
	}

	public static Fun<ChrLngPair, ChrLngPair> map1(Lng_Lng fun) {
		return pair -> of(pair.t0, fun.apply(pair.t1));
	}

	public static ChrLngPair of(char t0, long t1) {
		return new ChrLngPair(t0, t1);
	}

	private ChrLngPair(char t0, long t1) {
		this.t0 = t0;
		this.t1 = t1;
	}

	public static Comparator<ChrLngPair> comparator() {
		return (pair0, pair1) -> {
			int c = Boolean.compare(pair0 != null, pair1 != null);
			c = c == 0 ? Character.compare(pair0.t0, pair1.t0) : c;
			c = c == 0 ? Long.compare(pair0.t1, pair1.t1) : c;
			return c;
		};
	}

	public static Comparator<ChrLngPair> comparatorByFirst() {
		return (pair0, pair1) -> {
			int c = Boolean.compare(pair0 != null, pair1 != null);
			c = c == 0 ? Character.compare(pair0.t0, pair1.t0) : c;
			return c;
		};
	}

	public static char first_(ChrLngPair pair) {
		return pair.t0;
	}

	public static long second(ChrLngPair pair) {
		return pair.t1;
	}

	@Override
	public boolean equals(Object object) {
		if (Object_.clazz(object) == ChrLngPair.class) {
			ChrLngPair other = (ChrLngPair) object;
			return t0 == other.t0 && t1 == other.t1;
		} else
			return false;
	}

	@Override
	public int hashCode() {
		return Character.hashCode(t0) + 31 * Long.hashCode(t1);
	}

	@Override
	public String toString() {
		return t0 + ":" + t1;
	}

}
