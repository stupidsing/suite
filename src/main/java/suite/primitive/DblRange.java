package suite.primitive;

import java.util.Comparator;

import suite.object.Object_;
import suite.streamlet.FunUtil.Iterate;

public class DblRange {

	private static DblRange none_ = DblRange.of(DblFunUtil.EMPTYVALUE, DblFunUtil.EMPTYVALUE);

	public double s;
	public double e;

	public static Iterate<DblRange> mapFst(Dbl_Dbl fun) {
		return pair -> of(fun.apply(pair.s), pair.e);
	}

	public static Iterate<DblRange> mapSnd(Dbl_Dbl fun) {
		return pair -> of(pair.s, fun.apply(pair.e));
	}

	public static DblRange none() {
		return none_;
	}

	public static DblRange of(double s, double e) {
		return new DblRange(s, e);
	}

	protected DblRange(double s, double e) {
		update(s, e);
	}

	public static Comparator<DblRange> comparator() {
		return (pair0, pair1) -> {
			int c = Boolean.compare(pair0 != null, pair1 != null);
			c = c == 0 ? Double.compare(pair0.s, pair1.s) : c;
			c = c == 0 ? Double.compare(pair0.e, pair1.e) : c;
			return c;
		};
	}

	public static Comparator<DblRange> comparatorByFirst() {
		return (pair0, pair1) -> {
			int c = Boolean.compare(pair0 != null, pair1 != null);
			c = c == 0 ? Double.compare(pair0.s, pair1.s) : c;
			return c;
		};
	}

	public static double fst(DblRange pair) {
		return pair.s;
	}

	public static double snd(DblRange pair) {
		return pair.e;
	}

	public <O> O map(DblDbl_Obj<O> fun) {
		return fun.apply(s, e);
	}

	public void update(double s_, double e_) {
		s = s_;
		e = e_;
	}

	@Override
	public boolean equals(Object object) {
		if (Object_.clazz(object) == DblRange.class) {
			var other = (DblRange) object;
			return s == other.s && e == other.e;
		} else
			return false;
	}

	@Override
	public int hashCode() {
		return Double.hashCode(s) + 31 * Double.hashCode(e);
	}

	@Override
	public String toString() {
		return s + ":" + e;
	}

}
