package suite.util;

import java.lang.reflect.Array;

public class Array_ {

	@SafeVarargs
	public static <T> T[] concat(Class<T> clazz, T[]... lists) {
		var size = 0;

		for (var list : lists)
			size += list.length;

		var result = Array_.newArray(clazz, size);
		var i = 0;

		for (var list : lists) {
			var length = list.length;
			Array_.copy(list, 0, result, i, length);
			i += length;
		}

		return result;
	}

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
