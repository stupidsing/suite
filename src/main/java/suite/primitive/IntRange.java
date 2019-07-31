package suite.primitive;

import java.util.Comparator;

import primal.Ob;
import suite.streamlet.FunUtil.Iterate;

public class IntRange {

	private static IntRange none_ = IntRange.of(IntFunUtil.EMPTYVALUE, IntFunUtil.EMPTYVALUE);

	public int s;
	public int e;

	public static Iterate<IntRange> mapFst(Int_Int fun) {
		return range -> of(fun.apply(range.s), range.e);
	}

	public static Iterate<IntRange> mapSnd(Int_Int fun) {
		return range -> of(range.s, fun.apply(range.e));
	}

	public static IntRange none() {
		return none_;
	}

	public static IntRange of(int s, int e) {
		return new IntRange(s, e);
	}

	protected IntRange(int s, int e) {
		update(s, e);
	}

	public static Comparator<IntRange> comparator() {
		return (range0, range1) -> {
			var c = Boolean.compare(range0 != null, range1 != null);
			c = c == 0 ? Integer.compare(range0.s, range1.s) : c;
			c = c == 0 ? Integer.compare(range0.e, range1.e) : c;
			return c;
		};
	}

	public static Comparator<IntRange> comparatorByFirst() {
		return (range0, range1) -> {
			var c = Boolean.compare(range0 != null, range1 != null);
			c = c == 0 ? Integer.compare(range0.s, range1.s) : c;
			return c;
		};
	}

	public static int fst(IntRange range) {
		return range.s;
	}

	public static int snd(IntRange range) {
		return range.e;
	}

	public int length() {
		return (int) (e - s);
	}

	public <O> O map(IntInt_Obj<O> fun) {
		return fun.apply(s, e);
	}

	public void update(int s_, int e_) {
		s = s_;
		e = e_;
	}

	@Override
	public boolean equals(Object object) {
		if (Ob.clazz(object) == IntRange.class) {
			var other = (IntRange) object;
			return s == other.s && e == other.e;
		} else
			return false;
	}

	@Override
	public int hashCode() {
		return Integer.hashCode(s) + 31 * Integer.hashCode(e);
	}

	@Override
	public String toString() {
		return s + ":" + e;
	}

}
