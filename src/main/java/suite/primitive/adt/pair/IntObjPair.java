package suite.primitive.adt.pair;

import java.util.Comparator;
import java.util.Objects;

import primal.Ob;
import primal.fp.Funs.Fun;
import primal.fp.Funs.Iterate;
import suite.primitive.IntFunUtil;
import suite.primitive.IntPrimitives.IntObj_Obj;
import suite.primitive.Int_Int;

public class IntObjPair<V> {

	private static IntObjPair<?> none_ = IntObjPair.of(IntFunUtil.EMPTYVALUE, null);

	public int k;
	public V v;

	public static <V> Iterate<IntObjPair<V>> mapFst(Int_Int fun) {
		return pair -> of(fun.apply(pair.k), pair.v);
	}

	public static <V0, V1> Fun<IntObjPair<V0>, IntObjPair<V1>> mapSnd(Fun<V0, V1> fun) {
		return pair -> of(pair.k, fun.apply(pair.v));
	}

	@SuppressWarnings("unchecked")
	public static <V> IntObjPair<V> none() {
		return (IntObjPair<V>) none_;
	}

	public static <V> IntObjPair<V> of(int k, V v) {
		return new IntObjPair<>(k, v);
	}

	protected IntObjPair(int k, V v) {
		this.k = k;
		this.v = v;
	}

	public static <V extends Comparable<? super V>> Comparator<IntObjPair<V>> comparator() {
		return (pair0, pair1) -> {
			var c = Boolean.compare(pair0 != null, pair1 != null);
			c = c == 0 ? Integer.compare(pair0.k, pair1.k) : c;
			c = c == 0 ? Ob.compare(pair0.v, pair1.v) : c;
			return c;
		};
	}

	public static <V> Comparator<IntObjPair<V>> comparatorByFirst() {
		return (pair0, pair1) -> {
			var c = Boolean.compare(pair0 != null, pair1 != null);
			c = c == 0 ? Integer.compare(pair0.k, pair1.k) : c;
			return c;
		};
	}

	public static int fst(IntObjPair<?> pair) {
		return pair.k;
	}

	public static <T> T snd(IntObjPair<T> pair) {
		return pair != null ? pair.v : null;
	}

	public <O> O map(IntObj_Obj<V, O> fun) {
		return fun.apply(k, v);
	}

	public void update(int k_, V v_) {
		k = k_;
		v = v_;
	}

	@Override
	public boolean equals(Object object) {
		if (Ob.clazz(object) == IntObjPair.class) {
			var other = (IntObjPair<?>) object;
			return k == other.k && Ob.equals(v, other.v);
		} else
			return false;
	}

	@Override
	public int hashCode() {
		return Integer.hashCode(k) + 31 * Objects.hashCode(v);
	}

	@Override
	public String toString() {
		return k + ":" + v;
	}

}
