package suite.util;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import suite.streamlet.Read;

public class Set_ {

	@SafeVarargs
	public static <T> Set<T> set(T... ts) {
		Set<T> set = new HashSet<>();
		for (T t : ts)
			set.add(t);
		return set;
	}

	public static <T> Set<T> intersect(Collection<Collection<T>> collections) {
		List<HashSet<T>> sets = Read.from(collections).map(HashSet<T>::new).toList();
		Set<T> union = union_(collections);
		Set<T> missing = new HashSet<>();

		for (Set<T> set : sets)
			for (T t : union)
				if (!set.contains(t))
					missing.add(t);

		union.removeAll(missing);
		return union;
	}

	@SafeVarargs
	public static <T> Set<T> union(Collection<T>... collections) {
		return union_(Arrays.asList(collections));
	}

	private static <T> Set<T> union_(Collection<Collection<T>> collections) {
		Set<T> set = new HashSet<>();
		for (Collection<T> collection : collections)
			set.addAll(collection);
		return set;
	}

}
