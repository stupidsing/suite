package suite.adt.pair;

import java.util.Comparator;
import java.util.Objects;

import suite.primitive.PrimitiveFun.Char_Char;
import suite.util.FunUtil.Fun;
import suite.util.Object_;

public class CharCharPair {

	public char t0;
	public char t1;

	public static Fun<CharCharPair, CharCharPair> map0(Char_Char fun) {
		return pair -> CharCharPair.of(fun.apply(pair.t0), pair.t1);
	}

	public static Fun<CharCharPair, CharCharPair> map1(Char_Char fun) {
		return pair -> CharCharPair.of(pair.t0, fun.apply(pair.t1));
	}

	public static CharCharPair of(char t0, char t1) {
		return new CharCharPair(t0, t1);
	}

	private CharCharPair(char t0, char t1) {
		this.t0 = t0;
		this.t1 = t1;
	}

	public static Comparator<CharCharPair> comparator() {
		return (pair0, pair1) -> {
			int c = Boolean.compare(pair0 != null, pair1 != null);
			c = c == 0 ? Character.compare(pair0.t0, pair1.t0) : c;
			c = c == 0 ? Character.compare(pair0.t1, pair1.t1) : c;
			return c;
		};
	}

	public static Comparator<CharCharPair> comparatorByFirst() {
		return (pair0, pair1) -> {
			int c = Boolean.compare(pair0 != null, pair1 != null);
			c = c == 0 ? Character.compare(pair0.t0, pair1.t0) : c;
			return c;
		};
	}

	public static char first_(CharCharPair pair) {
		return pair.t0;
	}

	public static char second(CharCharPair pair) {
		return pair.t1;
	}

	@Override
	public boolean equals(Object object) {
		if (Object_.clazz(object) == CharCharPair.class) {
			CharCharPair other = (CharCharPair) object;
			return t0 == other.t0 && t1 == other.t1;
		} else
			return false;
	}

	@Override
	public int hashCode() {
		return Objects.hashCode(t0) ^ Objects.hashCode(t1);
	}

	@Override
	public String toString() {
		return t0 + ":" + t1;
	}

}
