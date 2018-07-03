package suite.adt.pair;

import java.util.Comparator;
import java.util.Objects;

import suite.streamlet.FunUtil.Fun;
import suite.streamlet.FunUtil2.Fun2;
import suite.util.Object_;

public class Pair<T0, T1> {

	private static Pair<?, ?> none_ = Pair.of(null, null);

	public T0 t0;
	public T1 t1;

	public static <K0, K1, V> Fun<Pair<K0, V>, Pair<K1, V>> mapFst(Fun<K0, K1> fun) {
		return pair -> of(fun.apply(pair.t0), pair.t1);
	}

	public static <K, V0, V1> Fun<Pair<K, V0>, Pair<K, V1>> mapSnd(Fun<V0, V1> fun) {
		return pair -> of(pair.t0, fun.apply(pair.t1));
	}

	@SuppressWarnings("unchecked")
	public static <T0, T1> Pair<T0, T1> none() {
		return (Pair<T0, T1>) none_;
	}

	public static <T0, T1> Pair<T0, T1> of(T0 t0, T1 t1) {
		return new Pair<>(t0, t1);
	}

	private Pair(T0 t0, T1 t1) {
		update(t0, t1);
	}

	public static <T0 extends Comparable<? super T0>, T1 extends Comparable<? super T1>> Comparator<Pair<T0, T1>> comparator() {
		return (pair0, pair1) -> {
			var c = Boolean.compare(pair0 != null, pair1 != null);
			c = c == 0 ? Object_.compare(pair0.t0, pair1.t0) : c;
			c = c == 0 ? Object_.compare(pair0.t1, pair1.t1) : c;
			return c;
		};
	}

	public static <T0 extends Comparable<? super T0>, T1> Comparator<Pair<T0, T1>> comparatorByFirst() {
		return (pair0, pair1) -> Object_.compare(fst(pair0), fst(pair1));
	}

	public static <T0> T0 fst(Pair<T0, ?> pair) {
		return pair != null ? pair.t0 : null;
	}

	public static <T1> T1 snd(Pair<?, T1> pair) {
		return pair != null ? pair.t1 : null;
	}

	public <O> O map(Fun2<T0, T1, O> fun) {
		return fun.apply(t0, t1);
	}

	public void update(T0 t0_, T1 t1_) {
		t0 = t0_;
		t1 = t1_;
	}

	@Override
	public boolean equals(Object object) {
		if (Object_.clazz(object) == Pair.class) {
			var other = (Pair<?, ?>) object;
			return Objects.equals(t0, other.t0) && Objects.equals(t1, other.t1);
		} else
			return false;
	}

	@Override
	public int hashCode() {
		return Objects.hashCode(t0) + 31 * Objects.hashCode(t1);
	}

	@Override
	public String toString() {
		return t0 + ":" + t1;
	}

}
