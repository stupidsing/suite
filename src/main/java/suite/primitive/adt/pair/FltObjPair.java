package suite.primitive.adt.pair;

import java.util.Comparator;
import java.util.Objects;

import suite.object.Object_;
import suite.primitive.FltFunUtil;
import suite.primitive.FltPrimitives.FltObj_Obj;
import suite.primitive.Flt_Flt;
import suite.streamlet.FunUtil.Fun;
import suite.streamlet.FunUtil.Iterate;

public class FltObjPair<V> {

	private static FltObjPair<?> none_ = FltObjPair.of(FltFunUtil.EMPTYVALUE, null);

	public float k;
	public V v;

	public static <V> Iterate<FltObjPair<V>> mapFst(Flt_Flt fun) {
		return pair -> of(fun.apply(pair.k), pair.v);
	}

	public static <V0, V1> Fun<FltObjPair<V0>, FltObjPair<V1>> mapSnd(Fun<V0, V1> fun) {
		return pair -> of(pair.k, fun.apply(pair.v));
	}

	@SuppressWarnings("unchecked")
	public static <V> FltObjPair<V> none() {
		return (FltObjPair<V>) none_;
	}

	public static <V> FltObjPair<V> of(float k, V v) {
		return new FltObjPair<>(k, v);
	}

	protected FltObjPair(float k, V v) {
		this.k = k;
		this.v = v;
	}

	public static <V extends Comparable<? super V>> Comparator<FltObjPair<V>> comparator() {
		return (pair0, pair1) -> {
			var c = Boolean.compare(pair0 != null, pair1 != null);
			c = c == 0 ? Float.compare(pair0.k, pair1.k) : c;
			c = c == 0 ? Object_.compare(pair0.v, pair1.v) : c;
			return c;
		};
	}

	public static <V> Comparator<FltObjPair<V>> comparatorByFirst() {
		return (pair0, pair1) -> {
			var c = Boolean.compare(pair0 != null, pair1 != null);
			c = c == 0 ? Float.compare(pair0.k, pair1.k) : c;
			return c;
		};
	}

	public static float fst(FltObjPair<?> pair) {
		return pair.k;
	}

	public static <T> T snd(FltObjPair<T> pair) {
		return pair != null ? pair.v : null;
	}

	public <O> O map(FltObj_Obj<V, O> fun) {
		return fun.apply(k, v);
	}

	public void update(float k_, V v_) {
		k = k_;
		v = v_;
	}

	@Override
	public boolean equals(Object object) {
		if (Object_.clazz(object) == FltObjPair.class) {
			var other = (FltObjPair<?>) object;
			return k == other.k && Objects.equals(v, other.v);
		} else
			return false;
	}

	@Override
	public int hashCode() {
		return Float.hashCode(k) + 31 * Objects.hashCode(v);
	}

	@Override
	public String toString() {
		return k + ":" + v;
	}

}
