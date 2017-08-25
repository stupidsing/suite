package suite.primitive.adt.pair;

import java.util.Comparator;

import suite.adt.Opt;
import suite.primitive.ChrFunUtil;
import suite.primitive.ChrInt_Obj;
import suite.primitive.Chr_Chr;
import suite.primitive.IntFunUtil;
import suite.primitive.Int_Int;
import suite.util.FunUtil.Iterate;
import suite.util.Object_;

public class ChrIntPair {

	private static ChrIntPair none_ = ChrIntPair.of(ChrFunUtil.EMPTYVALUE, IntFunUtil.EMPTYVALUE);

	public char t0;
	public int t1;

	public static Iterate<ChrIntPair> map0(Chr_Chr fun) {
		return pair -> of(fun.apply(pair.t0), pair.t1);
	}

	public static Iterate<ChrIntPair> map1(Int_Int fun) {
		return pair -> of(pair.t0, fun.apply(pair.t1));
	}

	public static ChrIntPair none() {
		return none_;
	}

	public static ChrIntPair of(char t0, int t1) {
		return new ChrIntPair(t0, t1);
	}

	private ChrIntPair(char t0, int t1) {
		this.t0 = t0;
		this.t1 = t1;
	}

	public static Comparator<ChrIntPair> comparator() {
		return (pair0, pair1) -> {
			int c = Boolean.compare(pair0 != null, pair1 != null);
			c = c == 0 ? Character.compare(pair0.t0, pair1.t0) : c;
			c = c == 0 ? Integer.compare(pair0.t1, pair1.t1) : c;
			return c;
		};
	}

	public static Comparator<ChrIntPair> comparatorByFirst() {
		return (pair0, pair1) -> {
			int c = Boolean.compare(pair0 != null, pair1 != null);
			c = c == 0 ? Character.compare(pair0.t0, pair1.t0) : c;
			return c;
		};
	}

	public static char first_(ChrIntPair pair) {
		return pair.t0;
	}

	public static int second(ChrIntPair pair) {
		return pair.t1;
	}

	public <O> Opt<O> map(ChrInt_Obj<O> fun) {
		return t0 != ChrFunUtil.EMPTYVALUE ? Opt.of(fun.apply(t0, t1)) : Opt.none();
	}

	public void update(char t0_, int t1_) {
		t0 = t0_;
		t1 = t1_;
	}

	@Override
	public boolean equals(Object object) {
		if (Object_.clazz(object) == ChrIntPair.class) {
			ChrIntPair other = (ChrIntPair) object;
			return t0 == other.t0 && t1 == other.t1;
		} else
			return false;
	}

	@Override
	public int hashCode() {
		return Character.hashCode(t0) + 31 * Integer.hashCode(t1);
	}

	@Override
	public String toString() {
		return t0 + ":" + t1;
	}

}
