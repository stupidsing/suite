package org.weiqi;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Random;

public class RandomList<T> extends ArrayList<T> implements Iterable<T> {

	private static final long serialVersionUID = 1l;

	private final static Random random = new Random();

	public RandomList() {
		super();
	}

	public RandomList(int n) {
		super(n);
	}

	public boolean add(T t) {
		int size = size();

		if (size > 0) {
			int position = random.nextInt(size);
			super.add(get(position));
			set(position, t);
		} else
			super.add(t);

		return true;
	}

	public T last() {
		int size = size();
		return size > 0 ? get(size - 1) : null;
	}

	public T remove() {
		int size = size();
		return size > 0 ? remove(size - 1) : null;
	}

	@Override
	public Iterator<T> iterator() {
		return new Iterator<T>() {
			private int i = size();

			public boolean hasNext() {
				return i > 0;
			}

			public T next() {
				return get(--i);
			}

			public void remove() {
				if (i > 0)
					set(i, RandomList.this.remove());
				else
					RandomList.this.remove(0);
			}
		};
	}

	public static void setSeed(long seed) {
		random.setSeed(seed);
	}

}
