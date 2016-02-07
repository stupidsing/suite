package suite.adt;

import java.util.Comparator;

import suite.util.Util;

/**
 * Priority queue implementation from Programming Pearls.
 *
 * @author ywsing
 */
public class PriorityQueue<T> {

	private int size;
	private T ts[]; // ts[0] is not used
	private Comparator<T> comparator;

	public PriorityQueue(Class<T> clazz, int max, Comparator<T> comparator) {
		this.comparator = comparator;
		ts = Util.newArray(clazz, max + 1);
		size = 0;
	}

	public void insert(T t) {
		int i, p;
		ts[++size] = t;
		for (i = size; 1 < i && 0 < compare(ts[p = i / 2], ts[i]); i = p)
			swap(p, i);
	}

	public T extractMin() {
		int i, c;
		T t = ts[1];
		ts[1] = ts[size--];
		for (i = 1; (c = 2 * i) <= size; i = c) {
			if (c + 1 <= size && compare(ts[c + 1], ts[c]) < 0)
				c++;
			if (compare(ts[i], ts[c]) <= 0)
				break;
			swap(c, i);
		}
		return t;
	}

	private int compare(T t0, T t1) {
		return comparator.compare(t0, t1);
	}

	private void swap(int i0, int i1) {
		T temp = ts[i0];
		ts[i0] = ts[i1];
		ts[i1] = temp;
	}

}
