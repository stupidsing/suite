package suite.adt.pair;

import java.util.Comparator;
import java.util.Objects;

import suite.object.Object_;
import suite.streamlet.FunUtil.Fun;
import suite.streamlet.FunUtil2.Fun2;

public class Pair<K, V> {

	private static Pair<?, ?> none_ = Pair.of(null, null);

	public K k;
	public V v;

	public static <K0, K1, V> Fun<Pair<K0, V>, Pair<K1, V>> mapFst(Fun<K0, K1> fun) {
		return pair -> of(fun.apply(pair.k), pair.v);
	}

	public static <K, V0, V1> Fun<Pair<K, V0>, Pair<K, V1>> mapSnd(Fun<V0, V1> fun) {
		return pair -> of(pair.k, fun.apply(pair.v));
	}

	@SuppressWarnings("unchecked")
	public static <K, V> Pair<K, V> none() {
		return (Pair<K, V>) none_;
	}

	public static <K, V> Pair<K, V> of(K K, V v) {
		return new Pair<>(K, v);
	}

	protected Pair(K K, V v) {
		update(K, v);
	}

	public static <K extends Comparable<? super K>, V extends Comparable<? super V>> Comparator<Pair<K, V>> comparator() {
		return (pair0, pair1) -> {
			var c = Boolean.compare(pair0 != null, pair1 != null);
			c = c == 0 ? Object_.compare(pair0.k, pair1.k) : c;
			c = c == 0 ? Object_.compare(pair0.v, pair1.v) : c;
			return c;
		};
	}

	public static <K extends Comparable<? super K>, V> Comparator<Pair<K, V>> comparatorByFirst() {
		return Comparator.comparing(pair -> fst(pair), Object_::compare);
	}

	public static <K> K fst(Pair<K, ?> pair) {
		return pair != null ? pair.k : null;
	}

	public static <V> V snd(Pair<?, V> pair) {
		return pair != null ? pair.v : null;
	}

	public <O> O map(Fun2<K, V, O> fun) {
		return fun.apply(k, v);
	}

	public void update(K k_, V v_) {
		k = k_;
		v = v_;
	}

	@Override
	public boolean equals(Object object) {
		if (Object_.clazz(object) == Pair.class) {
			var other = (Pair<?, ?>) object;
			return Objects.equals(k, other.k) && Objects.equals(v, other.v);
		} else
			return false;
	}

	@Override
	public int hashCode() {
		return Objects.hashCode(k) + 31 * Objects.hashCode(v);
	}

	@Override
	public String toString() {
		return k + ":" + v;
	}

}
