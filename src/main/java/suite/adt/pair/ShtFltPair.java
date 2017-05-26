package suite.adt.pair;

import java.util.Comparator;

import suite.primitive.Flt_Flt;
import suite.primitive.Sht_Sht;
import suite.util.FunUtil.Fun;
import suite.util.Object_;

public class ShtFltPair {

	public short t0;
	public float t1;

	public static Fun<ShtFltPair, ShtFltPair> map0(Sht_Sht fun) {
		return pair -> of(fun.apply(pair.t0), pair.t1);
	}

	public static Fun<ShtFltPair, ShtFltPair> map1(Flt_Flt fun) {
		return pair -> of(pair.t0, fun.apply(pair.t1));
	}

	public static ShtFltPair of(short t0, float t1) {
		return new ShtFltPair(t0, t1);
	}

	private ShtFltPair(short t0, float t1) {
		this.t0 = t0;
		this.t1 = t1;
	}

	public static Comparator<ShtFltPair> comparator() {
		return (pair0, pair1) -> {
			int c = Boolean.compare(pair0 != null, pair1 != null);
			c = c == 0 ? Short.compare(pair0.t0, pair1.t0) : c;
			c = c == 0 ? Float.compare(pair0.t1, pair1.t1) : c;
			return c;
		};
	}

	public static Comparator<ShtFltPair> comparatorByFirst() {
		return (pair0, pair1) -> {
			int c = Boolean.compare(pair0 != null, pair1 != null);
			c = c == 0 ? Short.compare(pair0.t0, pair1.t0) : c;
			return c;
		};
	}

	public static short first_(ShtFltPair pair) {
		return pair.t0;
	}

	public static float second(ShtFltPair pair) {
		return pair.t1;
	}

	@Override
	public boolean equals(Object object) {
		if (Object_.clazz(object) == ShtFltPair.class) {
			ShtFltPair other = (ShtFltPair) object;
			return t0 == other.t0 && t1 == other.t1;
		} else
			return false;
	}

	@Override
	public int hashCode() {
		return Short.hashCode(t0) + 31 * Float.hashCode(t1);
	}

	@Override
	public String toString() {
		return t0 + ":" + t1;
	}

}
