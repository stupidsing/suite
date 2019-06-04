package suite.primitive.adt.pair;

import java.util.Comparator;

import suite.object.Object_;
import suite.primitive.FltDbl_Obj;
import suite.primitive.FltFunUtil;
import suite.primitive.Flt_Flt;
import suite.primitive.DblFunUtil;
import suite.primitive.Dbl_Dbl;
import suite.streamlet.FunUtil.Iterate;

public class FltDblPair {

	private static FltDblPair none_ = FltDblPair.of(FltFunUtil.EMPTYVALUE, DblFunUtil.EMPTYVALUE);

	public float t0;
	public double t1;

	public static Iterate<FltDblPair> mapFst(Flt_Flt fun) {
		return pair -> of(fun.apply(pair.t0), pair.t1);
	}

	public static Iterate<FltDblPair> mapSnd(Dbl_Dbl fun) {
		return pair -> of(pair.t0, fun.apply(pair.t1));
	}

	public static FltDblPair none() {
		return none_;
	}

	public static FltDblPair of(float t0, double t1) {
		return new FltDblPair(t0, t1);
	}

	protected FltDblPair(float t0, double t1) {
		update(t0, t1);
	}

	public static Comparator<FltDblPair> comparator() {
		return (pair0, pair1) -> {
			int c = Boolean.compare(pair0 != null, pair1 != null);
			c = c == 0 ? Float.compare(pair0.t0, pair1.t0) : c;
			c = c == 0 ? Double.compare(pair0.t1, pair1.t1) : c;
			return c;
		};
	}

	public static Comparator<FltDblPair> comparatorByFirst() {
		return (pair0, pair1) -> {
			int c = Boolean.compare(pair0 != null, pair1 != null);
			c = c == 0 ? Float.compare(pair0.t0, pair1.t0) : c;
			return c;
		};
	}

	public static float fst(FltDblPair pair) {
		return pair.t0;
	}

	public static double snd(FltDblPair pair) {
		return pair.t1;
	}

	public <O> O map(FltDbl_Obj<O> fun) {
		return fun.apply(t0, t1);
	}

	public void update(float t0_, double t1_) {
		t0 = t0_;
		t1 = t1_;
	}

	@Override
	public boolean equals(Object object) {
		if (Object_.clazz(object) == FltDblPair.class) {
			var other = (FltDblPair) object;
			return t0 == other.t0 && t1 == other.t1;
		} else
			return false;
	}

	@Override
	public int hashCode() {
		return Float.hashCode(t0) + 31 * Double.hashCode(t1);
	}

	@Override
	public String toString() {
		return t0 + ":" + t1;
	}

}
