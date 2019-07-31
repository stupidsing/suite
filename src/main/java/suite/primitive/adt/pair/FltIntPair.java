package suite.primitive.adt.pair;

import java.util.Comparator;

import primal.Ob;
import suite.primitive.FltFunUtil;
import suite.primitive.FltInt_Obj;
import suite.primitive.Flt_Flt;
import suite.primitive.IntFunUtil;
import suite.primitive.Int_Int;
import suite.streamlet.FunUtil.Iterate;

public class FltIntPair {

	private static FltIntPair none_ = FltIntPair.of(FltFunUtil.EMPTYVALUE, IntFunUtil.EMPTYVALUE);

	public float t0;
	public int t1;

	public static Iterate<FltIntPair> mapFst(Flt_Flt fun) {
		return pair -> of(fun.apply(pair.t0), pair.t1);
	}

	public static Iterate<FltIntPair> mapSnd(Int_Int fun) {
		return pair -> of(pair.t0, fun.apply(pair.t1));
	}

	public static FltIntPair none() {
		return none_;
	}

	public static FltIntPair of(float t0, int t1) {
		return new FltIntPair(t0, t1);
	}

	protected FltIntPair(float t0, int t1) {
		update(t0, t1);
	}

	public static Comparator<FltIntPair> comparator() {
		return (pair0, pair1) -> {
			var c = Boolean.compare(pair0 != null, pair1 != null);
			c = c == 0 ? Float.compare(pair0.t0, pair1.t0) : c;
			c = c == 0 ? Integer.compare(pair0.t1, pair1.t1) : c;
			return c;
		};
	}

	public static Comparator<FltIntPair> comparatorByFirst() {
		return (pair0, pair1) -> {
			var c = Boolean.compare(pair0 != null, pair1 != null);
			c = c == 0 ? Float.compare(pair0.t0, pair1.t0) : c;
			return c;
		};
	}

	public static float fst(FltIntPair pair) {
		return pair.t0;
	}

	public static int snd(FltIntPair pair) {
		return pair.t1;
	}

	public <O> O map(FltInt_Obj<O> fun) {
		return fun.apply(t0, t1);
	}

	public void update(float t0_, int t1_) {
		t0 = t0_;
		t1 = t1_;
	}

	@Override
	public boolean equals(Object object) {
		if (Ob.clazz(object) == FltIntPair.class) {
			var other = (FltIntPair) object;
			return t0 == other.t0 && t1 == other.t1;
		} else
			return false;
	}

	@Override
	public int hashCode() {
		return Float.hashCode(t0) + 31 * Integer.hashCode(t1);
	}

	@Override
	public String toString() {
		return t0 + ":" + t1;
	}

}
