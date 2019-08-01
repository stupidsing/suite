package suite.primitive.adt.pair;

import java.util.Comparator;

import primal.Verbs.Get;
import primal.fp.Funs.Iterate;
import primal.primitive.ChrPrim;
import primal.primitive.FltPrim;
import suite.primitive.Chr_Chr;
import suite.primitive.FltChr_Obj;
import suite.primitive.Flt_Flt;

public class FltChrPair {

	private static FltChrPair none_ = FltChrPair.of(FltPrim.EMPTYVALUE, ChrPrim.EMPTYVALUE);

	public float t0;
	public char t1;

	public static Iterate<FltChrPair> mapFst(Flt_Flt fun) {
		return pair -> of(fun.apply(pair.t0), pair.t1);
	}

	public static Iterate<FltChrPair> mapSnd(Chr_Chr fun) {
		return pair -> of(pair.t0, fun.apply(pair.t1));
	}

	public static FltChrPair none() {
		return none_;
	}

	public static FltChrPair of(float t0, char t1) {
		return new FltChrPair(t0, t1);
	}

	protected FltChrPair(float t0, char t1) {
		update(t0, t1);
	}

	public static Comparator<FltChrPair> comparator() {
		return (pair0, pair1) -> {
			var c = Boolean.compare(pair0 != null, pair1 != null);
			c = c == 0 ? Float.compare(pair0.t0, pair1.t0) : c;
			c = c == 0 ? Character.compare(pair0.t1, pair1.t1) : c;
			return c;
		};
	}

	public static Comparator<FltChrPair> comparatorByFirst() {
		return (pair0, pair1) -> {
			var c = Boolean.compare(pair0 != null, pair1 != null);
			c = c == 0 ? Float.compare(pair0.t0, pair1.t0) : c;
			return c;
		};
	}

	public static float fst(FltChrPair pair) {
		return pair.t0;
	}

	public static char snd(FltChrPair pair) {
		return pair.t1;
	}

	public <O> O map(FltChr_Obj<O> fun) {
		return fun.apply(t0, t1);
	}

	public void update(float t0_, char t1_) {
		t0 = t0_;
		t1 = t1_;
	}

	@Override
	public boolean equals(Object object) {
		if (Get.clazz(object) == FltChrPair.class) {
			var other = (FltChrPair) object;
			return t0 == other.t0 && t1 == other.t1;
		} else
			return false;
	}

	@Override
	public int hashCode() {
		return Float.hashCode(t0) + 31 * Character.hashCode(t1);
	}

	@Override
	public String toString() {
		return t0 + ":" + t1;
	}

}
