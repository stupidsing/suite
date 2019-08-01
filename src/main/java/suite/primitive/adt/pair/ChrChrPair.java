package suite.primitive.adt.pair;

import java.util.Comparator;

import primal.Verbs.Get;
import primal.fp.Funs.Iterate;
import primal.primitive.ChrPrim;
import suite.primitive.ChrChr_Obj;
import suite.primitive.Chr_Chr;

public class ChrChrPair {

	private static ChrChrPair none_ = ChrChrPair.of(ChrPrim.EMPTYVALUE, ChrPrim.EMPTYVALUE);

	public char t0;
	public char t1;

	public static Iterate<ChrChrPair> mapFst(Chr_Chr fun) {
		return pair -> of(fun.apply(pair.t0), pair.t1);
	}

	public static Iterate<ChrChrPair> mapSnd(Chr_Chr fun) {
		return pair -> of(pair.t0, fun.apply(pair.t1));
	}

	public static ChrChrPair none() {
		return none_;
	}

	public static ChrChrPair of(char t0, char t1) {
		return new ChrChrPair(t0, t1);
	}

	protected ChrChrPair(char t0, char t1) {
		update(t0, t1);
	}

	public static Comparator<ChrChrPair> comparator() {
		return (pair0, pair1) -> {
			var c = Boolean.compare(pair0 != null, pair1 != null);
			c = c == 0 ? Character.compare(pair0.t0, pair1.t0) : c;
			c = c == 0 ? Character.compare(pair0.t1, pair1.t1) : c;
			return c;
		};
	}

	public static Comparator<ChrChrPair> comparatorByFirst() {
		return (pair0, pair1) -> {
			var c = Boolean.compare(pair0 != null, pair1 != null);
			c = c == 0 ? Character.compare(pair0.t0, pair1.t0) : c;
			return c;
		};
	}

	public static char fst(ChrChrPair pair) {
		return pair.t0;
	}

	public static char snd(ChrChrPair pair) {
		return pair.t1;
	}

	public <O> O map(ChrChr_Obj<O> fun) {
		return fun.apply(t0, t1);
	}

	public void update(char t0_, char t1_) {
		t0 = t0_;
		t1 = t1_;
	}

	@Override
	public boolean equals(Object object) {
		if (Get.clazz(object) == ChrChrPair.class) {
			var other = (ChrChrPair) object;
			return t0 == other.t0 && t1 == other.t1;
		} else
			return false;
	}

	@Override
	public int hashCode() {
		return Character.hashCode(t0) + 31 * Character.hashCode(t1);
	}

	@Override
	public String toString() {
		return t0 + ":" + t1;
	}

}
