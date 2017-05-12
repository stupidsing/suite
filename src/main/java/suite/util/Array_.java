package suite.util;

import java.lang.reflect.Array;

public class Array_ {

	@SuppressWarnings("unchecked")
	public static <T> T[] newArray(Class<T> clazz, int dim) {
		return (T[]) Array.newInstance(clazz, dim);
	}

}
