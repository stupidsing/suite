package suite.primitive.adt.pair;

import java.util.Comparator;

import suite.object.Object_;
import suite.primitive.ChrLng_Obj;
import suite.primitive.ChrFunUtil;
import suite.primitive.Chr_Chr;
import suite.primitive.LngFunUtil;
import suite.primitive.Lng_Lng;
import suite.streamlet.FunUtil.Iterate;

public class ChrLngPair {

	private static ChrLngPair none_ = ChrLngPair.of(ChrFunUtil.EMPTYVALUE, LngFunUtil.EMPTYVALUE);

	public char t0;
	public long t1;

	public static Iterate<ChrLngPair> mapFst(Chr_Chr fun) {
		return pair -> of(fun.apply(pair.t0), pair.t1);
	}

	public static Iterate<ChrLngPair> mapSnd(Lng_Lng fun) {
		return pair -> of(pair.t0, fun.apply(pair.t1));
	}

	public static ChrLngPair none() {
		return none_;
	}

	public static ChrLngPair of(char t0, long t1) {
		return new ChrLngPair(t0, t1);
	}

	protected ChrLngPair(char t0, long t1) {
		update(t0, t1);
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

	public static char fst(ChrLngPair pair) {
		return pair.t0;
	}

	public static long snd(ChrLngPair pair) {
		return pair.t1;
	}

	public <O> O map(ChrLng_Obj<O> fun) {
		return fun.apply(t0, t1);
	}

	public void update(char t0_, long t1_) {
		t0 = t0_;
		t1 = t1_;
	}

	@Override
	public boolean equals(Object object) {
		if (Object_.clazz(object) == ChrLngPair.class) {
			var other = (ChrLngPair) object;
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
