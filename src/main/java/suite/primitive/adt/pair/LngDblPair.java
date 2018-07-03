package suite.primitive.adt.pair;

import java.util.Comparator;

import suite.primitive.DblFunUtil;
import suite.primitive.Dbl_Dbl;
import suite.primitive.LngDbl_Obj;
import suite.primitive.LngFunUtil;
import suite.primitive.Lng_Lng;
import suite.streamlet.FunUtil.Iterate;
import suite.util.Object_;

public class LngDblPair {

	private static LngDblPair none_ = LngDblPair.of(LngFunUtil.EMPTYVALUE, DblFunUtil.EMPTYVALUE);

	public long t0;
	public double t1;

	public static Iterate<LngDblPair> mapFst(Lng_Lng fun) {
		return pair -> of(fun.apply(pair.t0), pair.t1);
	}

	public static Iterate<LngDblPair> mapSnd(Dbl_Dbl fun) {
		return pair -> of(pair.t0, fun.apply(pair.t1));
	}

	public static LngDblPair none() {
		return none_;
	}

	public static LngDblPair of(long t0, double t1) {
		return new LngDblPair(t0, t1);
	}

	private LngDblPair(long t0, double t1) {
		update(t0, t1);
	}

	public static Comparator<LngDblPair> comparator() {
		return (pair0, pair1) -> {
			int c = Boolean.compare(pair0 != null, pair1 != null);
			c = c == 0 ? Long.compare(pair0.t0, pair1.t0) : c;
			c = c == 0 ? Double.compare(pair0.t1, pair1.t1) : c;
			return c;
		};
	}

	public static Comparator<LngDblPair> comparatorByFirst() {
		return (pair0, pair1) -> {
			int c = Boolean.compare(pair0 != null, pair1 != null);
			c = c == 0 ? Long.compare(pair0.t0, pair1.t0) : c;
			return c;
		};
	}

	public static long fst(LngDblPair pair) {
		return pair.t0;
	}

	public static double snd(LngDblPair pair) {
		return pair.t1;
	}

	public <O> O map(LngDbl_Obj<O> fun) {
		return fun.apply(t0, t1);
	}

	public void update(long t0_, double t1_) {
		t0 = t0_;
		t1 = t1_;
	}

	@Override
	public boolean equals(Object object) {
		if (Object_.clazz(object) == LngDblPair.class) {
			var other = (LngDblPair) object;
			return t0 == other.t0 && t1 == other.t1;
		} else
			return false;
	}

	@Override
	public int hashCode() {
		return Long.hashCode(t0) + 31 * Double.hashCode(t1);
	}

	@Override
	public String toString() {
		return t0 + ":" + t1;
	}

}
