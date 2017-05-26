package suite.adt.pair;

import java.util.Comparator;

import suite.primitive.Sht_Sht;
import suite.primitive.Int_Int;
import suite.util.FunUtil.Fun;
import suite.util.Object_;

public class ShtIntPair {

	public short t0;
	public int t1;

	public static Fun<ShtIntPair, ShtIntPair> map0(Sht_Sht fun) {
		return pair -> of(fun.apply(pair.t0), pair.t1);
	}

	public static Fun<ShtIntPair, ShtIntPair> map1(Int_Int fun) {
		return pair -> of(pair.t0, fun.apply(pair.t1));
	}

	public static ShtIntPair of(short t0, int t1) {
		return new ShtIntPair(t0, t1);
	}

	private ShtIntPair(short t0, int t1) {
		this.t0 = t0;
		this.t1 = t1;
	}

	public static Comparator<ShtIntPair> comparator() {
		return (pair0, pair1) -> {
			int c = Boolean.compare(pair0 != null, pair1 != null);
			c = c == 0 ? Short.compare(pair0.t0, pair1.t0) : c;
			c = c == 0 ? Integer.compare(pair0.t1, pair1.t1) : c;
			return c;
		};
	}

	public static Comparator<ShtIntPair> comparatorByFirst() {
		return (pair0, pair1) -> {
			int c = Boolean.compare(pair0 != null, pair1 != null);
			c = c == 0 ? Short.compare(pair0.t0, pair1.t0) : c;
			return c;
		};
	}

	public static short first_(ShtIntPair pair) {
		return pair.t0;
	}

	public static int second(ShtIntPair pair) {
		return pair.t1;
	}

	@Override
	public boolean equals(Object object) {
		if (Object_.clazz(object) == ShtIntPair.class) {
			ShtIntPair other = (ShtIntPair) object;
			return t0 == other.t0 && t1 == other.t1;
		} else
			return false;
	}

	@Override
	public int hashCode() {
		return Short.hashCode(t0) + 31 * Integer.hashCode(t1);
	}

	@Override
	public String toString() {
		return t0 + ":" + t1;
	}

}
