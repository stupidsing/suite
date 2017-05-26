package suite.adt.pair;

import java.util.Comparator;

import suite.primitive.Sht_Sht;
import suite.util.FunUtil.Fun;
import suite.util.Object_;

public class ShtShtPair {

	public short t0;
	public short t1;

	public static Fun<ShtShtPair, ShtShtPair> map0(Sht_Sht fun) {
		return pair -> of(fun.apply(pair.t0), pair.t1);
	}

	public static Fun<ShtShtPair, ShtShtPair> map1(Sht_Sht fun) {
		return pair -> of(pair.t0, fun.apply(pair.t1));
	}

	public static ShtShtPair of(short t0, short t1) {
		return new ShtShtPair(t0, t1);
	}

	private ShtShtPair(short t0, short t1) {
		this.t0 = t0;
		this.t1 = t1;
	}

	public static Comparator<ShtShtPair> comparator() {
		return (pair0, pair1) -> {
			int c = Boolean.compare(pair0 != null, pair1 != null);
			c = c == 0 ? Short.compare(pair0.t0, pair1.t0) : c;
			c = c == 0 ? Short.compare(pair0.t1, pair1.t1) : c;
			return c;
		};
	}

	public static Comparator<ShtShtPair> comparatorByFirst() {
		return (pair0, pair1) -> {
			int c = Boolean.compare(pair0 != null, pair1 != null);
			c = c == 0 ? Short.compare(pair0.t0, pair1.t0) : c;
			return c;
		};
	}

	public static short first_(ShtShtPair pair) {
		return pair.t0;
	}

	public static short second(ShtShtPair pair) {
		return pair.t1;
	}

	@Override
	public boolean equals(Object object) {
		if (Object_.clazz(object) == ShtShtPair.class) {
			ShtShtPair other = (ShtShtPair) object;
			return t0 == other.t0 && t1 == other.t1;
		} else
			return false;
	}

	@Override
	public int hashCode() {
		return Short.hashCode(t0) + 31 * Short.hashCode(t1);
	}

	@Override
	public String toString() {
		return t0 + ":" + t1;
	}

}
