package suite.primitive.adt.pair;

import java.util.Comparator;

import primal.Verbs.Get;
import primal.fp.Funs.Iterate;
import suite.primitive.FltFunUtil;
import suite.primitive.Flt_Flt;
import suite.primitive.IntFlt_Obj;
import suite.primitive.IntFunUtil;
import suite.primitive.Int_Int;

public class IntFltPair {

	private static IntFltPair none_ = IntFltPair.of(IntFunUtil.EMPTYVALUE, FltFunUtil.EMPTYVALUE);

	public int t0;
	public float t1;

	public static Iterate<IntFltPair> mapFst(Int_Int fun) {
		return pair -> of(fun.apply(pair.t0), pair.t1);
	}

	public static Iterate<IntFltPair> mapSnd(Flt_Flt fun) {
		return pair -> of(pair.t0, fun.apply(pair.t1));
	}

	public static IntFltPair none() {
		return none_;
	}

	public static IntFltPair of(int t0, float t1) {
		return new IntFltPair(t0, t1);
	}

	protected IntFltPair(int t0, float t1) {
		update(t0, t1);
	}

	public static Comparator<IntFltPair> comparator() {
		return (pair0, pair1) -> {
			var c = Boolean.compare(pair0 != null, pair1 != null);
			c = c == 0 ? Integer.compare(pair0.t0, pair1.t0) : c;
			c = c == 0 ? Float.compare(pair0.t1, pair1.t1) : c;
			return c;
		};
	}

	public static Comparator<IntFltPair> comparatorByFirst() {
		return (pair0, pair1) -> {
			var c = Boolean.compare(pair0 != null, pair1 != null);
			c = c == 0 ? Integer.compare(pair0.t0, pair1.t0) : c;
			return c;
		};
	}

	public static int fst(IntFltPair pair) {
		return pair.t0;
	}

	public static float snd(IntFltPair pair) {
		return pair.t1;
	}

	public <O> O map(IntFlt_Obj<O> fun) {
		return fun.apply(t0, t1);
	}

	public void update(int t0_, float t1_) {
		t0 = t0_;
		t1 = t1_;
	}

	@Override
	public boolean equals(Object object) {
		if (Get.clazz(object) == IntFltPair.class) {
			var other = (IntFltPair) object;
			return t0 == other.t0 && t1 == other.t1;
		} else
			return false;
	}

	@Override
	public int hashCode() {
		return Integer.hashCode(t0) + 31 * Float.hashCode(t1);
	}

	@Override
	public String toString() {
		return t0 + ":" + t1;
	}

}
