package suite.primitive.adt.pair;

import java.util.Comparator;
import java.util.Objects;

import primal.Verbs.Compare;
import primal.Verbs.Equals;
import primal.Verbs.Get;
import primal.fp.Funs.Fun;
import primal.fp.Funs.Iterate;
import suite.primitive.ChrFunUtil;
import suite.primitive.ChrPrimitives.ChrObj_Obj;
import suite.primitive.Chr_Chr;

public class ChrObjPair<V> {

	private static ChrObjPair<?> none_ = ChrObjPair.of(ChrFunUtil.EMPTYVALUE, null);

	public char k;
	public V v;

	public static <V> Iterate<ChrObjPair<V>> mapFst(Chr_Chr fun) {
		return pair -> of(fun.apply(pair.k), pair.v);
	}

	public static <V0, V1> Fun<ChrObjPair<V0>, ChrObjPair<V1>> mapSnd(Fun<V0, V1> fun) {
		return pair -> of(pair.k, fun.apply(pair.v));
	}

	@SuppressWarnings("unchecked")
	public static <V> ChrObjPair<V> none() {
		return (ChrObjPair<V>) none_;
	}

	public static <V> ChrObjPair<V> of(char k, V v) {
		return new ChrObjPair<>(k, v);
	}

	protected ChrObjPair(char k, V v) {
		this.k = k;
		this.v = v;
	}

	public static <V extends Comparable<? super V>> Comparator<ChrObjPair<V>> comparator() {
		return (pair0, pair1) -> {
			var c = Boolean.compare(pair0 != null, pair1 != null);
			c = c == 0 ? Character.compare(pair0.k, pair1.k) : c;
			c = c == 0 ? Compare.objects(pair0.v, pair1.v) : c;
			return c;
		};
	}

	public static <V> Comparator<ChrObjPair<V>> comparatorByFirst() {
		return (pair0, pair1) -> {
			var c = Boolean.compare(pair0 != null, pair1 != null);
			c = c == 0 ? Character.compare(pair0.k, pair1.k) : c;
			return c;
		};
	}

	public static char fst(ChrObjPair<?> pair) {
		return pair.k;
	}

	public static <T> T snd(ChrObjPair<T> pair) {
		return pair != null ? pair.v : null;
	}

	public <O> O map(ChrObj_Obj<V, O> fun) {
		return fun.apply(k, v);
	}

	public void update(char k_, V v_) {
		k = k_;
		v = v_;
	}

	@Override
	public boolean equals(Object object) {
		if (Get.clazz(object) == ChrObjPair.class) {
			var other = (ChrObjPair<?>) object;
			return k == other.k && Equals.ab(v, other.v);
		} else
			return false;
	}

	@Override
	public int hashCode() {
		return Character.hashCode(k) + 31 * Objects.hashCode(v);
	}

	@Override
	public String toString() {
		return k + ":" + v;
	}

}
