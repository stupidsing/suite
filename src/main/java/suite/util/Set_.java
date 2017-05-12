package suite.util;

import java.util.HashSet;
import java.util.Set;

public class Set_ {

	@SafeVarargs
	public static <T> Set<T> set(T... ts) {
		Set<T> set = new HashSet<>();
		for (T t : ts)
			set.add(t);
		return set;
	}

}
