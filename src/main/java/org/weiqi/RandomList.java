package org.weiqi;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

public class RandomList<T> implements Iterable<T> {

	private final static Random random = new Random();

	private List<T> list;

	public RandomList() {
		list = new ArrayList<T>();
	}

	public RandomList(int n) {
		list = new ArrayList<T>(Weiqi.AREA);
	}

	public void add(T t) {
		int size = list.size();

		if (size > 0) {
			int position = random.nextInt(size);
			list.add(list.get(position));
			list.set(position, t);
		} else
			list.add(t);
	}

	public T get() {
		int size = list.size();
		return size > 0 ? list.get(size - 1) : null;
	}

	public T remove() {
		int size = list.size();
		return size > 0 ? list.remove(size - 1) : null;
	}

	@Override
	public Iterator<T> iterator() {
		final int size = list.size();

		return new Iterator<T>() {
			int i = 0;

			public boolean hasNext() {
				return i < size;
			}

			public T next() {
				return list.get(i++);
			}

			public void remove() {
				if (i != size)
					list.set(i - 1, RandomList.this.remove());
				else
					list.remove(0);
			}
		};
	}

	public boolean isEmpty() {
		return list.isEmpty();
	}

	public int size() {
		return list.size();
	}

	public static void setSeed(long seed) {
		random.setSeed(seed);
	}

}
