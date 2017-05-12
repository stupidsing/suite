package suite.util;

import java.io.Closeable;
import java.io.IOException;
import java.util.Comparator;

public class Object_ {

	public static Class<?> clazz(Object object) {
		return object != null ? object.getClass() : null;
	}

	public static void closeQuietly(Closeable o) {
		if (o != null)
			try {
				o.close();
			} catch (IOException ex) {
				throw new RuntimeException(ex);
			}
	}

	public static <T extends Comparable<? super T>> Comparator<T> comparator() {
		return Object_::compare;
	}

	public static <T extends Comparable<? super T>> int compare(T t0, T t1) {
		boolean b0 = t0 != null;
		boolean b1 = t1 != null;
		if (b0 && b1)
			return t0.compareTo(t1);
		else
			return b0 ? 1 : b1 ? -1 : 0;
	}

	public static <T extends Comparable<? super T>> T min(T t0, T t1) {
		return compare(t0, t1) < 0 ? t0 : t1;
	}

	public static <T> Comparator<T> nullsFirst(Comparator<T> cmp0) {
		return (key0, key1) -> {
			boolean b0 = key0 != null;
			boolean b1 = key1 != null;

			if (b0 && b1)
				return cmp0.compare(key0, key1);
			else
				return b0 ? 1 : b1 ? -1 : 0;
		};
	}

	public static void wait(Object object) {
		wait(object, 0);
	}

	public static void wait(Object object, int timeOut) {
		try {
			object.wait(timeOut);
		} catch (InterruptedException e) {
		}
	}

}
