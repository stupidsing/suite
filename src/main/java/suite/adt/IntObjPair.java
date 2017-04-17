package suite.adt;

import java.util.Comparator;
import java.util.Objects;

import suite.primitive.PrimitiveFun.Int_Int;
import suite.util.FunUtil.Fun;
import suite.util.Util;

public class IntObjPair<T> {

	public int t0;
	public T t1;

	public static <V> Fun<IntObjPair<V>, IntObjPair<V>> map0(Int_Int fun) {
		return pair -> IntObjPair.of(fun.apply(pair.t0), pair.t1);
	}

	public static <V0, V1> Fun<IntObjPair<V0>, IntObjPair<V1>> map1(Fun<V0, V1> fun) {
		return pair -> IntObjPair.of(pair.t0, fun.apply(pair.t1));
	}

	public static <T> IntObjPair<T> of(int t0, T t1) {
		return new IntObjPair<>(t0, t1);
	}

	private IntObjPair(int t0, T t1) {
		this.t0 = t0;
		this.t1 = t1;
	}

	public static <T extends Comparable<? super T>> Comparator<IntObjPair<T>> comparator() {
		return (pair0, pair1) -> {
			int c = 0;
			c = c == 0 ? Integer.compare(first_(pair0), first_(pair1)) : c;
			c = c == 0 ? Util.compare(second(pair0), second(pair1)) : c;
			return c;
		};
	}

	public static <T> Comparator<IntObjPair<T>> comparatorByFirst() {
		return (pair0, pair1) -> Integer.compare(first_(pair0), first_(pair1));
	}

	public static int first_(IntObjPair<?> pair) {
		return pair != null ? pair.t0 : null;
	}

	public static <T> T second(IntObjPair<T> pair) {
		return pair != null ? pair.t1 : null;
	}

	@Override
	public boolean equals(Object object) {
		if (Util.clazz(object) == IntObjPair.class) {
			IntObjPair<?> other = (IntObjPair<?>) object;
			return t0 == other.t0 && Objects.equals(t1, other.t1);
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
