package suite.primitive.adt.pair;

import java.util.Comparator;
import java.util.Objects;

import primal.Verbs.Compare;
import primal.Verbs.Equals;
import primal.Verbs.Get;
import primal.fp.Funs.Fun;
import primal.fp.Funs.Iterate;
import suite.primitive.DblFunUtil;
import suite.primitive.DblPrimitives.DblObj_Obj;
import suite.primitive.Dbl_Dbl;

public class DblObjPair<V> {

	private static DblObjPair<?> none_ = DblObjPair.of(DblFunUtil.EMPTYVALUE, null);

	public double k;
	public V v;

	public static <V> Iterate<DblObjPair<V>> mapFst(Dbl_Dbl fun) {
		return pair -> of(fun.apply(pair.k), pair.v);
	}

	public static <V0, V1> Fun<DblObjPair<V0>, DblObjPair<V1>> mapSnd(Fun<V0, V1> fun) {
		return pair -> of(pair.k, fun.apply(pair.v));
	}

	@SuppressWarnings("unchecked")
	public static <V> DblObjPair<V> none() {
		return (DblObjPair<V>) none_;
	}

	public static <V> DblObjPair<V> of(double k, V v) {
		return new DblObjPair<>(k, v);
	}

	protected DblObjPair(double k, V v) {
		this.k = k;
		this.v = v;
	}

	public static <V extends Comparable<? super V>> Comparator<DblObjPair<V>> comparator() {
		return (pair0, pair1) -> {
			var c = Boolean.compare(pair0 != null, pair1 != null);
			c = c == 0 ? Double.compare(pair0.k, pair1.k) : c;
			c = c == 0 ? Compare.objects(pair0.v, pair1.v) : c;
			return c;
		};
	}

	public static <V> Comparator<DblObjPair<V>> comparatorByFirst() {
		return (pair0, pair1) -> {
			var c = Boolean.compare(pair0 != null, pair1 != null);
			c = c == 0 ? Double.compare(pair0.k, pair1.k) : c;
			return c;
		};
	}

	public static double fst(DblObjPair<?> pair) {
		return pair.k;
	}

	public static <T> T snd(DblObjPair<T> pair) {
		return pair != null ? pair.v : null;
	}

	public <O> O map(DblObj_Obj<V, O> fun) {
		return fun.apply(k, v);
	}

	public void update(double k_, V v_) {
		k = k_;
		v = v_;
	}

	@Override
	public boolean equals(Object object) {
		if (Get.clazz(object) == DblObjPair.class) {
			var other = (DblObjPair<?>) object;
			return k == other.k && Equals.ab(v, other.v);
		} else
			return false;
	}

	@Override
	public int hashCode() {
		return Double.hashCode(k) + 31 * Objects.hashCode(v);
	}

	@Override
	public String toString() {
		return k + ":" + v;
	}

}
