package suite.adt;

import java.util.Comparator;
import java.util.Objects;

import suite.primitive.PrimitiveFun.Int_Int;
import suite.util.FunUtil.Fun;
import suite.util.Util;

public class ObjIntPair<T> {

	public T t0;
	public int t1;

	public static <V0, V1> Fun<ObjIntPair<V0>, ObjIntPair<V1>> map0(Fun<V0, V1> fun) {
		return pair -> ObjIntPair.of(fun.apply(pair.t0), pair.t1);
	}

	public static <V> Fun<ObjIntPair<V>, ObjIntPair<V>> map1(Int_Int fun) {
		return pair -> ObjIntPair.of(pair.t0, fun.apply(pair.t1));
	}

	public static <T> ObjIntPair<T> of(T t0, int t1) {
		return new ObjIntPair<>(t0, t1);
	}

	private ObjIntPair(T t0, int t1) {
		this.t0 = t0;
		this.t1 = t1;
	}

	public static <T extends Comparable<? super T>> Comparator<ObjIntPair<T>> comparator() {
		return (pair0, pair1) -> {
			int c = 0;
			c = c == 0 ? Util.compare(first_(pair0), first_(pair1)) : c;
			c = c == 0 ? Integer.compare(second(pair0), second(pair1)) : c;
			return c;
		};
	}

	public static <T> Comparator<ObjIntPair<T>> comparatorBySecond() {
		return (pair0, pair1) -> Util.compare(second(pair0), second(pair1));
	}

	public static <T> T first_(ObjIntPair<T> pair) {
		return pair != null ? pair.t0 : null;
	}

	public static int second(ObjIntPair<?> pair) {
		return pair.t1;
	}

	@Override
	public boolean equals(Object object) {
		if (Util.clazz(object) == ObjIntPair.class) {
			ObjIntPair<?> other = (ObjIntPair<?>) object;
			return Objects.equals(t0, other.t0) && t1 == other.t1;
		} else
			return false;
	}

	@Override
	public int hashCode() {
		return Objects.hashCode(t0) ^ Objects.hashCode(t1);
	}

	@Override
	public String toString() {
		return t0 + ":" + t1;
	}

}
