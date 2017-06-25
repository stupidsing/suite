package suite.adt.pair;

import java.util.Comparator;

import suite.adt.Opt;
import suite.primitive.ChrFunUtil;
import suite.primitive.Chr_Chr;
import suite.primitive.FltChr_Obj;
import suite.primitive.FltFunUtil;
import suite.primitive.Flt_Flt;
import suite.util.FunUtil.Fun;
import suite.util.Object_;

public class FltChrPair {

	private static FltChrPair none_ = FltChrPair.of(FltFunUtil.EMPTYVALUE, ChrFunUtil.EMPTYVALUE);

	public float t0;
	public char t1;

	public static Fun<FltChrPair, FltChrPair> map0(Flt_Flt fun) {
		return pair -> of(fun.apply(pair.t0), pair.t1);
	}

	public static Fun<FltChrPair, FltChrPair> map1(Chr_Chr fun) {
		return pair -> of(pair.t0, fun.apply(pair.t1));
	}

	public static FltChrPair none() {
		return none_;
	}

	public static FltChrPair of(float t0, char t1) {
		return new FltChrPair(t0, t1);
	}

	private FltChrPair(float t0, char t1) {
		this.t0 = t0;
		this.t1 = t1;
	}

	public static Comparator<FltChrPair> comparator() {
		return (pair0, pair1) -> {
			int c = Boolean.compare(pair0 != null, pair1 != null);
			c = c == 0 ? Float.compare(pair0.t0, pair1.t0) : c;
			c = c == 0 ? Character.compare(pair0.t1, pair1.t1) : c;
			return c;
		};
	}

	public static Comparator<FltChrPair> comparatorByFirst() {
		return (pair0, pair1) -> {
			int c = Boolean.compare(pair0 != null, pair1 != null);
			c = c == 0 ? Float.compare(pair0.t0, pair1.t0) : c;
			return c;
		};
	}

	public <O> Opt<O> map(FltChr_Obj<O> fun) {
		return t0 != FltFunUtil.EMPTYVALUE ? Opt.of(fun.apply(t0, t1)) : Opt.none();
	}

	public static float first_(FltChrPair pair) {
		return pair.t0;
	}

	public static char second(FltChrPair pair) {
		return pair.t1;
	}

	@Override
	public boolean equals(Object object) {
		if (Object_.clazz(object) == FltChrPair.class) {
			FltChrPair other = (FltChrPair) object;
			return t0 == other.t0 && t1 == other.t1;
		} else
			return false;
	}

	@Override
	public int hashCode() {
		return Float.hashCode(t0) + 31 * Character.hashCode(t1);
	}

	@Override
	public String toString() {
		return t0 + ":" + t1;
	}

}
