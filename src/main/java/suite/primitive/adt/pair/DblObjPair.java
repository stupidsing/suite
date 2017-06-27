package suite.primitive.adt.pair;

import java.util.Comparator;
import java.util.Objects;

import suite.adt.Opt;
import suite.primitive.DblFunUtil;
import suite.primitive.DblPrimitives.DblObj_Obj;
import suite.primitive.Dbl_Dbl;
import suite.util.FunUtil.Fun;
import suite.util.Object_;

public class DblObjPair<T> {

	private static DblObjPair<?> none_ = DblObjPair.of(DblFunUtil.EMPTYVALUE, null);

	public double t0;
	public T t1;

	public static <V> Fun<DblObjPair<V>, DblObjPair<V>> map0(Dbl_Dbl fun) {
		return pair -> of(fun.apply(pair.t0), pair.t1);
	}

	public static <V0, V1> Fun<DblObjPair<V0>, DblObjPair<V1>> map1(Fun<V0, V1> fun) {
		return pair -> of(pair.t0, fun.apply(pair.t1));
	}

	@SuppressWarnings("unchecked")
	public static <T> DblObjPair<T> none() {
		return (DblObjPair<T>) none_;
	}

	public static <T> DblObjPair<T> of(double t0, T t1) {
		return new DblObjPair<>(t0, t1);
	}

	private DblObjPair(double t0, T t1) {
		this.t0 = t0;
		this.t1 = t1;
	}

	public static <T extends Comparable<? super T>> Comparator<DblObjPair<T>> comparator() {
		return (pair0, pair1) -> {
			int c = Boolean.compare(pair0 != null, pair1 != null);
			c = c == 0 ? Double.compare(pair0.t0, pair1.t0) : c;
			c = c == 0 ? Object_.compare(pair0.t1, pair1.t1) : c;
			return c;
		};
	}

	public static <T> Comparator<DblObjPair<T>> comparatorByFirst() {
		return (pair0, pair1) -> {
			int c = Boolean.compare(pair0 != null, pair1 != null);
			c = c == 0 ? Double.compare(pair0.t0, pair1.t0) : c;
			return c;
		};
	}

	public <O> Opt<O> map(DblObj_Obj<T, O> fun) {
		return t0 != DblFunUtil.EMPTYVALUE ? Opt.of(fun.apply(t0, t1)) : Opt.none();
	}

	public static double first_(DblObjPair<?> pair) {
		return pair.t0;
	}

	public static <T> T second(DblObjPair<T> pair) {
		return pair != null ? pair.t1 : null;
	}

	@Override
	public boolean equals(Object object) {
		if (Object_.clazz(object) == DblObjPair.class) {
			DblObjPair<?> other = (DblObjPair<?>) object;
			return t0 == other.t0 && Objects.equals(t1, other.t1);
		} else
			return false;
	}

	@Override
	public int hashCode() {
		return Double.hashCode(t0) + 31 * Objects.hashCode(t1);
	}

	@Override
	public String toString() {
		return t0 + ":" + t1;
	}

}
