package suite.adt.pair;

import java.util.Comparator;

import suite.primitive.Chr_Chr;
import suite.primitive.Sht_Sht;
import suite.util.FunUtil.Fun;
import suite.util.Object_;

public class ChrShtPair {

	public char t0;
	public short t1;

	public static Fun<ChrShtPair, ChrShtPair> map0(Chr_Chr fun) {
		return pair -> of(fun.apply(pair.t0), pair.t1);
	}

	public static Fun<ChrShtPair, ChrShtPair> map1(Sht_Sht fun) {
		return pair -> of(pair.t0, fun.apply(pair.t1));
	}

	public static ChrShtPair of(char t0, short t1) {
		return new ChrShtPair(t0, t1);
	}

	private ChrShtPair(char t0, short t1) {
		this.t0 = t0;
		this.t1 = t1;
	}

	public static Comparator<ChrShtPair> comparator() {
		return (pair0, pair1) -> {
			int c = Boolean.compare(pair0 != null, pair1 != null);
			c = c == 0 ? Character.compare(pair0.t0, pair1.t0) : c;
			c = c == 0 ? Short.compare(pair0.t1, pair1.t1) : c;
			return c;
		};
	}

	public static Comparator<ChrShtPair> comparatorByFirst() {
		return (pair0, pair1) -> {
			int c = Boolean.compare(pair0 != null, pair1 != null);
			c = c == 0 ? Character.compare(pair0.t0, pair1.t0) : c;
			return c;
		};
	}

	public static char first_(ChrShtPair pair) {
		return pair.t0;
	}

	public static short second(ChrShtPair pair) {
		return pair.t1;
	}

	@Override
	public boolean equals(Object object) {
		if (Object_.clazz(object) == ChrShtPair.class) {
			ChrShtPair other = (ChrShtPair) object;
			return t0 == other.t0 && t1 == other.t1;
		} else
			return false;
	}

	@Override
	public int hashCode() {
		return Character.hashCode(t0) + 31 * Short.hashCode(t1);
	}

	@Override
	public String toString() {
		return t0 + ":" + t1;
	}

}
