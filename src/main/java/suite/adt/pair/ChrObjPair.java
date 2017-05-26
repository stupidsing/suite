package suite.adt.pair;

import java.util.Comparator;
import java.util.Objects;

import suite.primitive.Chr_Chr;
import suite.util.FunUtil.Fun;
import suite.util.Object_;

public class ChrObjPair<T> {

	public char t0;
	public T t1;

	public static <V> Fun<ChrObjPair<V>, ChrObjPair<V>> map0(Chr_Chr fun) {
		return pair -> of(fun.apply(pair.t0), pair.t1);
	}

	public static <V0, V1> Fun<ChrObjPair<V0>, ChrObjPair<V1>> map1(Fun<V0, V1> fun) {
		return pair -> of(pair.t0, fun.apply(pair.t1));
	}

	public static <T> ChrObjPair<T> of(char t0, T t1) {
		return new ChrObjPair<>(t0, t1);
	}

	private ChrObjPair(char t0, T t1) {
		this.t0 = t0;
		this.t1 = t1;
	}

	public static <T extends Comparable<? super T>> Comparator<ChrObjPair<T>> comparator() {
		return (pair0, pair1) -> {
			int c = Boolean.compare(pair0 != null, pair1 != null);
			c = c == 0 ? Character.compare(pair0.t0, pair1.t0) : c;
			c = c == 0 ? Object_.compare(pair0.t1, pair1.t1) : c;
			return c;
		};
	}

	public static <T> Comparator<ChrObjPair<T>> comparatorByFirst() {
		return (pair0, pair1) -> {
			int c = Boolean.compare(pair0 != null, pair1 != null);
			c = c == 0 ? Character.compare(pair0.t0, pair1.t0) : c;
			return c;
		};
	}

	public static char first_(ChrObjPair<?> pair) {
		return pair.t0;
	}

	public static <T> T second(ChrObjPair<T> pair) {
		return pair != null ? pair.t1 : null;
	}

	@Override
	public boolean equals(Object object) {
		if (Object_.clazz(object) == ChrObjPair.class) {
			ChrObjPair<?> other = (ChrObjPair<?>) object;
			return t0 == other.t0 && Objects.equals(t1, other.t1);
		} else
			return false;
	}

	@Override
	public int hashCode() {
		return Character.hashCode(t0) + 31 * Objects.hashCode(t1);
	}

	@Override
	public String toString() {
		return t0 + ":" + t1;
	}

}
