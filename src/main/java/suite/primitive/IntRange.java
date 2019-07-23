package suite.primitive;

import java.util.Comparator;

import suite.object.Object_;
import suite.streamlet.FunUtil.Iterate;

public class IntRange {

	private static IntRange none_ = IntRange.of(IntFunUtil.EMPTYVALUE, IntFunUtil.EMPTYVALUE);

	public int s;
	public int e;

	public static Iterate<IntRange> mapFst(Int_Int fun) {
		return pair -> of(fun.apply(pair.s), pair.e);
	}

	public static Iterate<IntRange> mapSnd(Int_Int fun) {
		return pair -> of(pair.s, fun.apply(pair.e));
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
		return (pair0, pair1) -> {
			int c = Boolean.compare(pair0 != null, pair1 != null);
			c = c == 0 ? Integer.compare(pair0.s, pair1.s) : c;
			c = c == 0 ? Integer.compare(pair0.e, pair1.e) : c;
			return c;
		};
	}

	public static Comparator<IntRange> comparatorByFirst() {
		return (pair0, pair1) -> {
			int c = Boolean.compare(pair0 != null, pair1 != null);
			c = c == 0 ? Integer.compare(pair0.s, pair1.s) : c;
			return c;
		};
	}

	public static int fst(IntRange pair) {
		return pair.s;
	}

	public static int snd(IntRange pair) {
		return pair.e;
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
		if (Object_.clazz(object) == IntRange.class) {
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
