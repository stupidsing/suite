package suite.util;

import static primal.statics.Fail.fail;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class Set_ {

	@SafeVarargs
	public static <T> Set<T> intersect(Collection<T>... collections) {
		return intersect(List.of(collections));
	}

	public static <T> Set<T> intersect(Collection<Collection<T>> collections) {
		var iter = collections.iterator();
		Set<T> set = iter.hasNext() ? new HashSet<>(iter.next()) : fail();
		while (iter.hasNext())
			set.retainAll(iter.next());
		return set;
	}

	@SafeVarargs
	public static <T> Set<T> union(Collection<T>... collections) {
		return union_(List.of(collections));
	}

	private static <T> Set<T> union_(Collection<Collection<T>> collections) {
		var set = new HashSet<T>();
		for (var collection : collections)
			set.addAll(collection);
		return set;
	}

}
