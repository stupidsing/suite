package suite.primitive;

import java.util.Comparator;

import suite.object.Object_;
import suite.streamlet.FunUtil.Iterate;

public class ChrRange {

	private static ChrRange none_ = ChrRange.of(ChrFunUtil.EMPTYVALUE, ChrFunUtil.EMPTYVALUE);

	public char s;
	public char e;

	public static Iterate<ChrRange> mapFst(Chr_Chr fun) {
		return pair -> of(fun.apply(pair.s), pair.e);
	}

	public static Iterate<ChrRange> mapSnd(Chr_Chr fun) {
		return pair -> of(pair.s, fun.apply(pair.e));
	}

	public static ChrRange none() {
		return none_;
	}

	public static ChrRange of(char s, char e) {
		return new ChrRange(s, e);
	}

	protected ChrRange(char s, char e) {
		update(s, e);
	}

	public static Comparator<ChrRange> comparator() {
		return (pair0, pair1) -> {
			int c = Boolean.compare(pair0 != null, pair1 != null);
			c = c == 0 ? Character.compare(pair0.s, pair1.s) : c;
			c = c == 0 ? Character.compare(pair0.e, pair1.e) : c;
			return c;
		};
	}

	public static Comparator<ChrRange> comparatorByFirst() {
		return (pair0, pair1) -> {
			int c = Boolean.compare(pair0 != null, pair1 != null);
			c = c == 0 ? Character.compare(pair0.s, pair1.s) : c;
			return c;
		};
	}

	public static char fst(ChrRange pair) {
		return pair.s;
	}

	public static char snd(ChrRange pair) {
		return pair.e;
	}

	public <O> O map(ChrChr_Obj<O> fun) {
		return fun.apply(s, e);
	}

	public void update(char s_, char e_) {
		s = s_;
		e = e_;
	}

	@Override
	public boolean equals(Object object) {
		if (Object_.clazz(object) == ChrRange.class) {
			var other = (ChrRange) object;
			return s == other.s && e == other.e;
		} else
			return false;
	}

	@Override
	public int hashCode() {
		return Character.hashCode(s) + 31 * Character.hashCode(e);
	}

	@Override
	public String toString() {
		return s + ":" + e;
	}

}
