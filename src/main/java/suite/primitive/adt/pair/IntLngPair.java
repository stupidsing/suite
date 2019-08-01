package suite.primitive.adt.pair;

import java.util.Comparator;

import primal.Verbs.Get;
import primal.fp.Funs.Iterate;
import primal.primitive.IntPrim;
import primal.primitive.LngPrim;
import suite.primitive.IntLng_Obj;
import suite.primitive.Int_Int;
import suite.primitive.Lng_Lng;

public class IntLngPair {

	private static IntLngPair none_ = IntLngPair.of(IntPrim.EMPTYVALUE, LngPrim.EMPTYVALUE);

	public int t0;
	public long t1;

	public static Iterate<IntLngPair> mapFst(Int_Int fun) {
		return pair -> of(fun.apply(pair.t0), pair.t1);
	}

	public static Iterate<IntLngPair> mapSnd(Lng_Lng fun) {
		return pair -> of(pair.t0, fun.apply(pair.t1));
	}

	public static IntLngPair none() {
		return none_;
	}

	public static IntLngPair of(int t0, long t1) {
		return new IntLngPair(t0, t1);
	}

	protected IntLngPair(int t0, long t1) {
		update(t0, t1);
	}

	public static Comparator<IntLngPair> comparator() {
		return (pair0, pair1) -> {
			var c = Boolean.compare(pair0 != null, pair1 != null);
			c = c == 0 ? Integer.compare(pair0.t0, pair1.t0) : c;
			c = c == 0 ? Long.compare(pair0.t1, pair1.t1) : c;
			return c;
		};
	}

	public static Comparator<IntLngPair> comparatorByFirst() {
		return (pair0, pair1) -> {
			var c = Boolean.compare(pair0 != null, pair1 != null);
			c = c == 0 ? Integer.compare(pair0.t0, pair1.t0) : c;
			return c;
		};
	}

	public static int fst(IntLngPair pair) {
		return pair.t0;
	}

	public static long snd(IntLngPair pair) {
		return pair.t1;
	}

	public <O> O map(IntLng_Obj<O> fun) {
		return fun.apply(t0, t1);
	}

	public void update(int t0_, long t1_) {
		t0 = t0_;
		t1 = t1_;
	}

	@Override
	public boolean equals(Object object) {
		if (Get.clazz(object) == IntLngPair.class) {
			var other = (IntLngPair) object;
			return t0 == other.t0 && t1 == other.t1;
		} else
			return false;
	}

	@Override
	public int hashCode() {
		return Integer.hashCode(t0) + 31 * Long.hashCode(t1);
	}

	@Override
	public String toString() {
		return t0 + ":" + t1;
	}

}
