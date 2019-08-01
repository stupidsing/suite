package suite.primitive.adt.pair;

import java.util.Comparator;

import primal.Verbs.Get;
import primal.fp.Funs.Iterate;
import suite.primitive.IntFunUtil;
import suite.primitive.IntInt_Obj;
import suite.primitive.Int_Int;

public class IntIntPair {

	private static IntIntPair none_ = IntIntPair.of(IntFunUtil.EMPTYVALUE, IntFunUtil.EMPTYVALUE);

	public int t0;
	public int t1;

	public static Iterate<IntIntPair> mapFst(Int_Int fun) {
		return pair -> of(fun.apply(pair.t0), pair.t1);
	}

	public static Iterate<IntIntPair> mapSnd(Int_Int fun) {
		return pair -> of(pair.t0, fun.apply(pair.t1));
	}

	public static IntIntPair none() {
		return none_;
	}

	public static IntIntPair of(int t0, int t1) {
		return new IntIntPair(t0, t1);
	}

	protected IntIntPair(int t0, int t1) {
		update(t0, t1);
	}

	public static Comparator<IntIntPair> comparator() {
		return (pair0, pair1) -> {
			var c = Boolean.compare(pair0 != null, pair1 != null);
			c = c == 0 ? Integer.compare(pair0.t0, pair1.t0) : c;
			c = c == 0 ? Integer.compare(pair0.t1, pair1.t1) : c;
			return c;
		};
	}

	public static Comparator<IntIntPair> comparatorByFirst() {
		return (pair0, pair1) -> {
			var c = Boolean.compare(pair0 != null, pair1 != null);
			c = c == 0 ? Integer.compare(pair0.t0, pair1.t0) : c;
			return c;
		};
	}

	public static int fst(IntIntPair pair) {
		return pair.t0;
	}

	public static int snd(IntIntPair pair) {
		return pair.t1;
	}

	public <O> O map(IntInt_Obj<O> fun) {
		return fun.apply(t0, t1);
	}

	public void update(int t0_, int t1_) {
		t0 = t0_;
		t1 = t1_;
	}

	@Override
	public boolean equals(Object object) {
		if (Get.clazz(object) == IntIntPair.class) {
			var other = (IntIntPair) object;
			return t0 == other.t0 && t1 == other.t1;
		} else
			return false;
	}

	@Override
	public int hashCode() {
		return Integer.hashCode(t0) + 31 * Integer.hashCode(t1);
	}

	@Override
	public String toString() {
		return t0 + ":" + t1;
	}

}
