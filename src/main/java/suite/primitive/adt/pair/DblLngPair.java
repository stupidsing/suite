package suite.primitive.adt.pair;

import java.util.Comparator;

import suite.object.Object_;
import suite.primitive.DblFunUtil;
import suite.primitive.DblLng_Obj;
import suite.primitive.Dbl_Dbl;
import suite.primitive.LngFunUtil;
import suite.primitive.Lng_Lng;
import suite.streamlet.FunUtil.Iterate;

public class DblLngPair {

	private static DblLngPair none_ = DblLngPair.of(DblFunUtil.EMPTYVALUE, LngFunUtil.EMPTYVALUE);

	public double t0;
	public long t1;

	public static Iterate<DblLngPair> mapFst(Dbl_Dbl fun) {
		return pair -> of(fun.apply(pair.t0), pair.t1);
	}

	public static Iterate<DblLngPair> mapSnd(Lng_Lng fun) {
		return pair -> of(pair.t0, fun.apply(pair.t1));
	}

	public static DblLngPair none() {
		return none_;
	}

	public static DblLngPair of(double t0, long t1) {
		return new DblLngPair(t0, t1);
	}

	protected DblLngPair(double t0, long t1) {
		update(t0, t1);
	}

	public static Comparator<DblLngPair> comparator() {
		return (pair0, pair1) -> {
			var c = Boolean.compare(pair0 != null, pair1 != null);
			c = c == 0 ? Double.compare(pair0.t0, pair1.t0) : c;
			c = c == 0 ? Long.compare(pair0.t1, pair1.t1) : c;
			return c;
		};
	}

	public static Comparator<DblLngPair> comparatorByFirst() {
		return (pair0, pair1) -> {
			var c = Boolean.compare(pair0 != null, pair1 != null);
			c = c == 0 ? Double.compare(pair0.t0, pair1.t0) : c;
			return c;
		};
	}

	public static double fst(DblLngPair pair) {
		return pair.t0;
	}

	public static long snd(DblLngPair pair) {
		return pair.t1;
	}

	public <O> O map(DblLng_Obj<O> fun) {
		return fun.apply(t0, t1);
	}

	public void update(double t0_, long t1_) {
		t0 = t0_;
		t1 = t1_;
	}

	@Override
	public boolean equals(Object object) {
		if (Object_.clazz(object) == DblLngPair.class) {
			var other = (DblLngPair) object;
			return t0 == other.t0 && t1 == other.t1;
		} else
			return false;
	}

	@Override
	public int hashCode() {
		return Double.hashCode(t0) + 31 * Long.hashCode(t1);
	}

	@Override
	public String toString() {
		return t0 + ":" + t1;
	}

}
