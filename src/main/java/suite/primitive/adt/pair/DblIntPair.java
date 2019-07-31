package suite.primitive.adt.pair;

import java.util.Comparator;

import primal.Ob;
import primal.fp.Funs.Iterate;
import suite.primitive.DblFunUtil;
import suite.primitive.DblInt_Obj;
import suite.primitive.Dbl_Dbl;
import suite.primitive.IntFunUtil;
import suite.primitive.Int_Int;

public class DblIntPair {

	private static DblIntPair none_ = DblIntPair.of(DblFunUtil.EMPTYVALUE, IntFunUtil.EMPTYVALUE);

	public double t0;
	public int t1;

	public static Iterate<DblIntPair> mapFst(Dbl_Dbl fun) {
		return pair -> of(fun.apply(pair.t0), pair.t1);
	}

	public static Iterate<DblIntPair> mapSnd(Int_Int fun) {
		return pair -> of(pair.t0, fun.apply(pair.t1));
	}

	public static DblIntPair none() {
		return none_;
	}

	public static DblIntPair of(double t0, int t1) {
		return new DblIntPair(t0, t1);
	}

	protected DblIntPair(double t0, int t1) {
		update(t0, t1);
	}

	public static Comparator<DblIntPair> comparator() {
		return (pair0, pair1) -> {
			var c = Boolean.compare(pair0 != null, pair1 != null);
			c = c == 0 ? Double.compare(pair0.t0, pair1.t0) : c;
			c = c == 0 ? Integer.compare(pair0.t1, pair1.t1) : c;
			return c;
		};
	}

	public static Comparator<DblIntPair> comparatorByFirst() {
		return (pair0, pair1) -> {
			var c = Boolean.compare(pair0 != null, pair1 != null);
			c = c == 0 ? Double.compare(pair0.t0, pair1.t0) : c;
			return c;
		};
	}

	public static double fst(DblIntPair pair) {
		return pair.t0;
	}

	public static int snd(DblIntPair pair) {
		return pair.t1;
	}

	public <O> O map(DblInt_Obj<O> fun) {
		return fun.apply(t0, t1);
	}

	public void update(double t0_, int t1_) {
		t0 = t0_;
		t1 = t1_;
	}

	@Override
	public boolean equals(Object object) {
		if (Ob.clazz(object) == DblIntPair.class) {
			var other = (DblIntPair) object;
			return t0 == other.t0 && t1 == other.t1;
		} else
			return false;
	}

	@Override
	public int hashCode() {
		return Double.hashCode(t0) + 31 * Integer.hashCode(t1);
	}

	@Override
	public String toString() {
		return t0 + ":" + t1;
	}

}
