package suite.primitive;

import java.util.Comparator;

import primal.Verbs.Get;
import primal.fp.Funs.Iterate;
import primal.primitive.ChrChr_Obj;
import primal.primitive.ChrPrim;

public class ChrRange {

	private static ChrRange none_ = ChrRange.of(ChrPrim.EMPTYVALUE, ChrPrim.EMPTYVALUE);

	public char s;
	public char e;

	public static Iterate<ChrRange> mapFst(Chr_Chr fun) {
		return range -> of(fun.apply(range.s), range.e);
	}

	public static Iterate<ChrRange> mapSnd(Chr_Chr fun) {
		return range -> of(range.s, fun.apply(range.e));
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
		return (range0, range1) -> {
			var c = Boolean.compare(range0 != null, range1 != null);
			c = c == 0 ? Character.compare(range0.s, range1.s) : c;
			c = c == 0 ? Character.compare(range0.e, range1.e) : c;
			return c;
		};
	}

	public static Comparator<ChrRange> comparatorByFirst() {
		return (range0, range1) -> {
			var c = Boolean.compare(range0 != null, range1 != null);
			c = c == 0 ? Character.compare(range0.s, range1.s) : c;
			return c;
		};
	}

	public static char fst(ChrRange range) {
		return range.s;
	}

	public static char snd(ChrRange range) {
		return range.e;
	}

	public char length() {
		return (char) (e - s);
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
		if (Get.clazz(object) == ChrRange.class) {
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
