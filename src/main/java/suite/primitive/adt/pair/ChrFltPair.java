package suite.primitive.adt.pair;

import java.util.Comparator;

import suite.adt.Opt;
import suite.primitive.ChrFlt_Obj;
import suite.primitive.ChrFunUtil;
import suite.primitive.Chr_Chr;
import suite.primitive.FltFunUtil;
import suite.primitive.Flt_Flt;
import suite.util.FunUtil.Iterate;
import suite.util.Object_;

public class ChrFltPair {

	private static ChrFltPair none_ = ChrFltPair.of(ChrFunUtil.EMPTYVALUE, FltFunUtil.EMPTYVALUE);

	public char t0;
	public float t1;

	public static Iterate<ChrFltPair> map0(Chr_Chr fun) {
		return pair -> of(fun.apply(pair.t0), pair.t1);
	}

	public static Iterate<ChrFltPair> map1(Flt_Flt fun) {
		return pair -> of(pair.t0, fun.apply(pair.t1));
	}

	public static ChrFltPair none() {
		return none_;
	}

	public static ChrFltPair of(char t0, float t1) {
		return new ChrFltPair(t0, t1);
	}

	private ChrFltPair(char t0, float t1) {
		this.t0 = t0;
		this.t1 = t1;
	}

	public static Comparator<ChrFltPair> comparator() {
		return (pair0, pair1) -> {
			int c = Boolean.compare(pair0 != null, pair1 != null);
			c = c == 0 ? Character.compare(pair0.t0, pair1.t0) : c;
			c = c == 0 ? Float.compare(pair0.t1, pair1.t1) : c;
			return c;
		};
	}

	public static Comparator<ChrFltPair> comparatorByFirst() {
		return (pair0, pair1) -> {
			int c = Boolean.compare(pair0 != null, pair1 != null);
			c = c == 0 ? Character.compare(pair0.t0, pair1.t0) : c;
			return c;
		};
	}

	public static char first_(ChrFltPair pair) {
		return pair.t0;
	}

	public static float second(ChrFltPair pair) {
		return pair.t1;
	}

	public <O> Opt<O> map(ChrFlt_Obj<O> fun) {
		return t0 != ChrFunUtil.EMPTYVALUE ? Opt.of(fun.apply(t0, t1)) : Opt.none();
	}

	public void update(char t0_, float t1_) {
		t0 = t0_;
		t1 = t1_;
	}

	@Override
	public boolean equals(Object object) {
		if (Object_.clazz(object) == ChrFltPair.class) {
			ChrFltPair other = (ChrFltPair) object;
			return t0 == other.t0 && t1 == other.t1;
		} else
			return false;
	}

	@Override
	public int hashCode() {
		return Character.hashCode(t0) + 31 * Float.hashCode(t1);
	}

	@Override
	public String toString() {
		return t0 + ":" + t1;
	}

}
