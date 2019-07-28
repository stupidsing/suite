package suite.util;

import static java.lang.Math.min;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import suite.streamlet.Read;
import suite.streamlet.Streamlet;

public class List_ {

	public static class Objs<T> extends ArrayList<T> {
		private static final long serialVersionUID = 1l;

		public Objs() {
		}

		public Objs(int capacity) {
			super(capacity);
		}

		public Objs(Collection<? extends T> c) {
			super(c);
		}

		public Streamlet<T> streamlet() {
			return Read.from(this);
		}
	}

	public static <T> List<List<T>> chunk(List<T> list, int n) {
		var s = 0;
		var subsets = new Objs<List<T>>();
		while (s < list.size()) {
			int s1 = min(s + n, list.size());
			subsets.add(list.subList(s, s1));
			s = s1;
		}
		return subsets;
	}

	@SafeVarargs
	public static <T> List<T> concat(Collection<T>... collections) {
		var list = new Objs<T>();
		for (var collection : collections)
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
		var size = list.size();
		if (pos < 0)
			pos += size;
		return list.subList(0, min(pos, size));
	}

	public static <T> Objs<T> of() {
		return new Objs<>();
	}

	public static <T> Objs<T> of(int capacity) {
		return new Objs<>(capacity);
	}

	public static <T> Objs<T> of(Class<T> clazz) {
		return new Objs<>();
	}

	public static <T> Objs<T> of(Collection<? extends T> c) {
		return new Objs<>(c);
	}

	public static <T> List<T> reverse(List<T> list0) {
		var list1 = new Objs<T>();
		for (var i = list0.size() - 1; 0 <= i; i--)
			list1.add(list0.get(i));
		return list1;
	}

	public static <T> List<T> right(List<T> list, int pos) {
		var size = list.size();
		if (pos < 0)
			pos += size;
		return list.subList(min(pos, size), size);
	}

	public static <T extends Comparable<? super T>> List<T> sort(Collection<T> list) {
		var list1 = new Objs<>(list);
		Collections.sort(list1);
		return list1;
	}

	public static <T> List<T> sort(Collection<T> list, Comparator<? super T> comparator) {
		var list1 = new Objs<>(list);
		Collections.sort(list1, comparator);
		return list1;
	}

}
