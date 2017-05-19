package suite.util;

import java.util.Collection;
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

	@SafeVarargs
	public static <T> Set<T> union(Collection<T>... collections) {
		Set<T> set = new HashSet<>();
		for (Collection<T> collection : collections)
			set.addAll(collection);
		return set;
	}

}
