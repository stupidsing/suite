package suite.primitive;

import java.util.Comparator;

import primal.Verbs.Get;
import primal.fp.Funs.Iterate;
import primal.primitive.DblPrim;

public class DblRange {

	private static DblRange none_ = DblRange.of(DblPrim.EMPTYVALUE, DblPrim.EMPTYVALUE);

	public double s;
	public double e;

	public static Iterate<DblRange> mapFst(Dbl_Dbl fun) {
		return range -> of(fun.apply(range.s), range.e);
	}

	public static Iterate<DblRange> mapSnd(Dbl_Dbl fun) {
		return range -> of(range.s, fun.apply(range.e));
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
		return (range0, range1) -> {
			var c = Boolean.compare(range0 != null, range1 != null);
			c = c == 0 ? Double.compare(range0.s, range1.s) : c;
			c = c == 0 ? Double.compare(range0.e, range1.e) : c;
			return c;
		};
	}

	public static Comparator<DblRange> comparatorByFirst() {
		return (range0, range1) -> {
			var c = Boolean.compare(range0 != null, range1 != null);
			c = c == 0 ? Double.compare(range0.s, range1.s) : c;
			return c;
		};
	}

	public static double fst(DblRange range) {
		return range.s;
	}

	public static double snd(DblRange range) {
		return range.e;
	}

	public double length() {
		return (double) (e - s);
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
		if (Get.clazz(object) == DblRange.class) {
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
