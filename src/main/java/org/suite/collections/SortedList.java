package org.suite.collections;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class SortedList<T> {

	private List<T> list = new ArrayList<T>();
	private Comparator<T> comparator;

	public SortedList(Comparator<T> comparator) {
		this.comparator = comparator;
	}

	public static <T> SortedList<T> create(Comparator<T> comparator) {
		return new SortedList<T>(comparator);
	}

	public void add(T t) {
		list.add(findPosition(t), t);
	}

	public int findPosition(T t) {
		int i, size = list.size();
		for (i = 0; i < size; i++)
			if (comparator.compare(list.get(i), t) >= 0)
				break;
		return i;
	}

	public T get(int index) {
		return list.get(index);
	}

	public boolean isEmpty() {
		return list.isEmpty();
	}

	public T remove(int index) {
		return list.remove(index);
	}

	public boolean remove(T t) {
		return list.remove(t);
	}

	public int size() {
		return list.size();
	}

}
