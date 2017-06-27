package suite.primitive.adt.pair;

import java.util.Comparator;

import suite.adt.Opt;
import suite.primitive.ChrFunUtil;
import suite.primitive.Chr_Chr;
import suite.primitive.LngChr_Obj;
import suite.primitive.LngFunUtil;
import suite.primitive.Lng_Lng;
import suite.util.FunUtil.Fun;
import suite.util.Object_;

public class LngChrPair {

	private static LngChrPair none_ = LngChrPair.of(LngFunUtil.EMPTYVALUE, ChrFunUtil.EMPTYVALUE);

	public long t0;
	public char t1;

	public static Fun<LngChrPair, LngChrPair> map0(Lng_Lng fun) {
		return pair -> of(fun.apply(pair.t0), pair.t1);
	}

	public static Fun<LngChrPair, LngChrPair> map1(Chr_Chr fun) {
		return pair -> of(pair.t0, fun.apply(pair.t1));
	}

	public static LngChrPair none() {
		return none_;
	}

	public static LngChrPair of(long t0, char t1) {
		return new LngChrPair(t0, t1);
	}

	private LngChrPair(long t0, char t1) {
		this.t0 = t0;
		this.t1 = t1;
	}

	public static Comparator<LngChrPair> comparator() {
		return (pair0, pair1) -> {
			int c = Boolean.compare(pair0 != null, pair1 != null);
			c = c == 0 ? Long.compare(pair0.t0, pair1.t0) : c;
			c = c == 0 ? Character.compare(pair0.t1, pair1.t1) : c;
			return c;
		};
	}

	public static Comparator<LngChrPair> comparatorByFirst() {
		return (pair0, pair1) -> {
			int c = Boolean.compare(pair0 != null, pair1 != null);
			c = c == 0 ? Long.compare(pair0.t0, pair1.t0) : c;
			return c;
		};
	}

	public <O> Opt<O> map(LngChr_Obj<O> fun) {
		return t0 != LngFunUtil.EMPTYVALUE ? Opt.of(fun.apply(t0, t1)) : Opt.none();
	}

	public static long first_(LngChrPair pair) {
		return pair.t0;
	}

	public static char second(LngChrPair pair) {
		return pair.t1;
	}

	@Override
	public boolean equals(Object object) {
		if (Object_.clazz(object) == LngChrPair.class) {
			LngChrPair other = (LngChrPair) object;
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
