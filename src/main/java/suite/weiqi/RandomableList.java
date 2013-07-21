package suite.weiqi;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Random;

public class RandomableList<T> extends ArrayList<T> implements Iterable<T> {

	private static final long serialVersionUID = 1l;

	private static final Random random = new Random();

	public RandomableList() {
		super();
	}

	public RandomableList(int n) {
		super(n);
	}

	public void addByRandomSwap(T t) {
		int size = size();

		if (size > 0) {
			int position = random.nextInt(size);
			add(get(position));
			set(position, t);
		} else
			add(t);
	}

	public T first() {
		return size() > 0 ? get(0) : null;
	}

	public T last() {
		int size = size();
		return size > 0 ? get(size - 1) : null;
	}

	public T removeBySwap(int i) {
		int size = size();
		T removed, last;

		if (size > i) {
			removed = get(i);
			last = removeLast();
			if (size > i + 1)
				set(i, last);
		} else
			removed = null;

		return removed;
	}

	public T removeLast() {
		return remove(size() - 1);
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
				RandomableList.this.removeBySwap(i);
			}
		};
	}

	public static void setSeed(long seed) {
		random.setSeed(seed);
	}

}
