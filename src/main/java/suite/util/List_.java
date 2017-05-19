package suite.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class List_ {

	@SafeVarargs
	public static <T> List<T> concat(Collection<T>... collections) {
		List<T> list = new ArrayList<>();
		for (Collection<T> collection : collections)
			list.addAll(collection);
		return list;
	}

	public static <T> T first(Collection<T> c) {
		return !c.isEmpty() ? c.iterator().next() : null;
	}

	public static <T> T last(List<T> c) {
		return !c.isEmpty() ? c.get(c.size() - 1) : null;
	}

	public static <T> List<T> left(List<T> list, int pos) {
		int size = list.size();
		if (pos < 0)
			pos += size;
		return list.subList(0, Math.min(pos, size));
	}

	public static <T> List<T> reverse(List<T> list0) {
		List<T> list1 = new ArrayList<>();
		for (int i = list0.size() - 1; 0 <= i; i--)
			list1.add(list0.get(i));
		return list1;
	}

	public static <T> List<T> right(List<T> list, int pos) {
		int size = list.size();
		if (pos < 0)
			pos += size;
		return list.subList(Math.min(pos, size), size);
	}

	public static <T extends Comparable<? super T>> List<T> sort(Collection<T> list) {
		List<T> list1 = new ArrayList<>(list);
		Collections.sort(list1);
		return list1;
	}

	public static <T> List<T> sort(Collection<T> list, Comparator<? super T> comparator) {
		List<T> list1 = new ArrayList<>(list);
		Collections.sort(list1, comparator);
		return list1;
	}

	public static <T> List<List<T>> splitn(List<T> list, int n) {
		int s = 0;
		List<List<T>> subsets = new ArrayList<>();
		while (s < list.size()) {
			int s1 = Math.min(s + n, list.size());
			subsets.add(list.subList(s, s1));
			s = s1;
		}
		return subsets;
	}

}
