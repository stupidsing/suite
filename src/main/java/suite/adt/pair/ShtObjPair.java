package suite.adt.pair;

import java.util.Comparator;
import java.util.Objects;

import suite.primitive.Sht_Sht;
import suite.util.FunUtil.Fun;
import suite.util.Object_;

public class ShtObjPair<T> {

	public short t0;
	public T t1;

	public static <V> Fun<ShtObjPair<V>, ShtObjPair<V>> map0(Sht_Sht fun) {
		return pair -> of(fun.apply(pair.t0), pair.t1);
	}

	public static <V0, V1> Fun<ShtObjPair<V0>, ShtObjPair<V1>> map1(Fun<V0, V1> fun) {
		return pair -> of(pair.t0, fun.apply(pair.t1));
	}

	public static <T> ShtObjPair<T> of(short t0, T t1) {
		return new ShtObjPair<>(t0, t1);
	}

	private ShtObjPair(short t0, T t1) {
		this.t0 = t0;
		this.t1 = t1;
	}

	public static <T extends Comparable<? super T>> Comparator<ShtObjPair<T>> comparator() {
		return (pair0, pair1) -> {
			int c = Boolean.compare(pair0 != null, pair1 != null);
			c = c == 0 ? Short.compare(pair0.t0, pair1.t0) : c;
			c = c == 0 ? Object_.compare(pair0.t1, pair1.t1) : c;
			return c;
		};
	}

	public static <T> Comparator<ShtObjPair<T>> comparatorByFirst() {
		return (pair0, pair1) -> {
			int c = Boolean.compare(pair0 != null, pair1 != null);
			c = c == 0 ? Short.compare(pair0.t0, pair1.t0) : c;
			return c;
		};
	}

	public static short first_(ShtObjPair<?> pair) {
		return pair.t0;
	}

	public static <T> T second(ShtObjPair<T> pair) {
		return pair != null ? pair.t1 : null;
	}

	@Override
	public boolean equals(Object object) {
		if (Object_.clazz(object) == ShtObjPair.class) {
			ShtObjPair<?> other = (ShtObjPair<?>) object;
			return t0 == other.t0 && Objects.equals(t1, other.t1);
		} else
			return false;
	}

	@Override
	public int hashCode() {
		return Short.hashCode(t0) + 31 * Objects.hashCode(t1);
	}

	@Override
	public String toString() {
		return t0 + ":" + t1;
	}

}
