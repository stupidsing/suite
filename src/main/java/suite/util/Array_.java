package suite.util;

import java.lang.reflect.Array;

public class Array_ {

	public static <T> void copy(T[] from, int fromIndex, T[] to, int toIndex, int size) {
		if (0 < size)
			System.arraycopy(from, fromIndex, to, toIndex, size);
		else if (size < 0)
			throw new IndexOutOfBoundsException();
	}

	@SuppressWarnings("unchecked")
	public static <T> T[] newArray(Class<T> clazz, int dim) {
		return (T[]) Array.newInstance(clazz, dim);
	}

}
