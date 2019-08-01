package suite.primitive.adt.pair;

import java.util.Comparator;

import primal.Verbs.Get;
import primal.fp.Funs.Iterate;
import primal.primitive.ChrPrim;
import primal.primitive.LngPrim;
import suite.primitive.Chr_Chr;
import suite.primitive.LngChr_Obj;
import suite.primitive.Lng_Lng;

public class LngChrPair {

	private static LngChrPair none_ = LngChrPair.of(LngPrim.EMPTYVALUE, ChrPrim.EMPTYVALUE);

	public long t0;
	public char t1;

	public static Iterate<LngChrPair> mapFst(Lng_Lng fun) {
		return pair -> of(fun.apply(pair.t0), pair.t1);
	}

	public static Iterate<LngChrPair> mapSnd(Chr_Chr fun) {
		return pair -> of(pair.t0, fun.apply(pair.t1));
	}

	public static LngChrPair none() {
		return none_;
	}

	public static LngChrPair of(long t0, char t1) {
		return new LngChrPair(t0, t1);
	}

	protected LngChrPair(long t0, char t1) {
		update(t0, t1);
	}

	public static Comparator<LngChrPair> comparator() {
		return (pair0, pair1) -> {
			var c = Boolean.compare(pair0 != null, pair1 != null);
			c = c == 0 ? Long.compare(pair0.t0, pair1.t0) : c;
			c = c == 0 ? Character.compare(pair0.t1, pair1.t1) : c;
			return c;
		};
	}

	public static Comparator<LngChrPair> comparatorByFirst() {
		return (pair0, pair1) -> {
			var c = Boolean.compare(pair0 != null, pair1 != null);
			c = c == 0 ? Long.compare(pair0.t0, pair1.t0) : c;
			return c;
		};
	}

	public static long fst(LngChrPair pair) {
		return pair.t0;
	}

	public static char snd(LngChrPair pair) {
		return pair.t1;
	}

	public <O> O map(LngChr_Obj<O> fun) {
		return fun.apply(t0, t1);
	}

	public void update(long t0_, char t1_) {
		t0 = t0_;
		t1 = t1_;
	}

	@Override
	public boolean equals(Object object) {
		if (Get.clazz(object) == LngChrPair.class) {
			var other = (LngChrPair) object;
			return t0 == other.t0 && t1 == other.t1;
		} else
			return false;
	}

	@Override
	public int hashCode() {
		return Long.hashCode(t0) + 31 * Character.hashCode(t1);
	}

	@Override
	public String toString() {
		return t0 + ":" + t1;
	}

}
