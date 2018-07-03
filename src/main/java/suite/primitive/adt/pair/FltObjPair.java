package suite.primitive.adt.pair;

import java.util.Comparator;
import java.util.Objects;

import suite.object.Object_;
import suite.primitive.FltFunUtil;
import suite.primitive.FltPrimitives.FltObj_Obj;
import suite.primitive.Flt_Flt;
import suite.streamlet.FunUtil.Fun;
import suite.streamlet.FunUtil.Iterate;

public class FltObjPair<T> {

	private static FltObjPair<?> none_ = FltObjPair.of(FltFunUtil.EMPTYVALUE, null);

	public float t0;
	public T t1;

	public static <V> Iterate<FltObjPair<V>> mapFst(Flt_Flt fun) {
		return pair -> of(fun.apply(pair.t0), pair.t1);
	}

	public static <V0, V1> Fun<FltObjPair<V0>, FltObjPair<V1>> mapSnd(Fun<V0, V1> fun) {
		return pair -> of(pair.t0, fun.apply(pair.t1));
	}

	@SuppressWarnings("unchecked")
	public static <T> FltObjPair<T> none() {
		return (FltObjPair<T>) none_;
	}

	public static <T> FltObjPair<T> of(float t0, T t1) {
		return new FltObjPair<>(t0, t1);
	}

	private FltObjPair(float t0, T t1) {
		this.t0 = t0;
		this.t1 = t1;
	}

	public static <T extends Comparable<? super T>> Comparator<FltObjPair<T>> comparator() {
		return (pair0, pair1) -> {
			var c = Boolean.compare(pair0 != null, pair1 != null);
			c = c == 0 ? Float.compare(pair0.t0, pair1.t0) : c;
			c = c == 0 ? Object_.compare(pair0.t1, pair1.t1) : c;
			return c;
		};
	}

	public static <T> Comparator<FltObjPair<T>> comparatorByFirst() {
		return (pair0, pair1) -> {
			var c = Boolean.compare(pair0 != null, pair1 != null);
			c = c == 0 ? Float.compare(pair0.t0, pair1.t0) : c;
			return c;
		};
	}

	public static float fst(FltObjPair<?> pair) {
		return pair.t0;
	}

	public static <T> T snd(FltObjPair<T> pair) {
		return pair != null ? pair.t1 : null;
	}

	public <O> O map(FltObj_Obj<T, O> fun) {
		return fun.apply(t0, t1);
	}

	public void update(float t0_, T t1_) {
		t0 = t0_;
		t1 = t1_;
	}

	@Override
	public boolean equals(Object object) {
		if (Object_.clazz(object) == FltObjPair.class) {
			var other = (FltObjPair<?>) object;
			return t0 == other.t0 && Objects.equals(t1, other.t1);
		} else
			return false;
	}

	@Override
	public int hashCode() {
		return Float.hashCode(t0) + 31 * Objects.hashCode(t1);
	}

	@Override
	public String toString() {
		return t0 + ":" + t1;
	}

}
