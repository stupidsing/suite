package suite.primitive.adt.pair;

import java.util.Comparator;

import primal.Verbs.Get;
import primal.fp.Funs.Iterate;
import suite.primitive.IntFunUtil;
import suite.primitive.Int_Int;
import suite.primitive.LngFunUtil;
import suite.primitive.LngInt_Obj;
import suite.primitive.Lng_Lng;

public class LngIntPair {

	private static LngIntPair none_ = LngIntPair.of(LngFunUtil.EMPTYVALUE, IntFunUtil.EMPTYVALUE);

	public long t0;
	public int t1;

	public static Iterate<LngIntPair> mapFst(Lng_Lng fun) {
		return pair -> of(fun.apply(pair.t0), pair.t1);
	}

	public static Iterate<LngIntPair> mapSnd(Int_Int fun) {
		return pair -> of(pair.t0, fun.apply(pair.t1));
	}

	public static LngIntPair none() {
		return none_;
	}

	public static LngIntPair of(long t0, int t1) {
		return new LngIntPair(t0, t1);
	}

	protected LngIntPair(long t0, int t1) {
		update(t0, t1);
	}

	public static Comparator<LngIntPair> comparator() {
		return (pair0, pair1) -> {
			var c = Boolean.compare(pair0 != null, pair1 != null);
			c = c == 0 ? Long.compare(pair0.t0, pair1.t0) : c;
			c = c == 0 ? Integer.compare(pair0.t1, pair1.t1) : c;
			return c;
		};
	}

	public static Comparator<LngIntPair> comparatorByFirst() {
		return (pair0, pair1) -> {
			var c = Boolean.compare(pair0 != null, pair1 != null);
			c = c == 0 ? Long.compare(pair0.t0, pair1.t0) : c;
			return c;
		};
	}

	public static long fst(LngIntPair pair) {
		return pair.t0;
	}

	public static int snd(LngIntPair pair) {
		return pair.t1;
	}

	public <O> O map(LngInt_Obj<O> fun) {
		return fun.apply(t0, t1);
	}

	public void update(long t0_, int t1_) {
		t0 = t0_;
		t1 = t1_;
	}

	@Override
	public boolean equals(Object object) {
		if (Get.clazz(object) == LngIntPair.class) {
			var other = (LngIntPair) object;
			return t0 == other.t0 && t1 == other.t1;
		} else
			return false;
	}

	@Override
	public int hashCode() {
		return Long.hashCode(t0) + 31 * Integer.hashCode(t1);
	}

	@Override
	public String toString() {
		return t0 + ":" + t1;
	}

}
