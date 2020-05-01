package suite.adt;

import primal.Verbs.New;

import java.util.Comparator;

/**
 * Priority queue implementation from Programming Pearls.
 *
 * @author ywsing
 */
public class PriorityQueue<T> {

	private int size;
	private T[] ts; // ts[0] is not used
	private Comparator<T> comparator;

	public PriorityQueue(Class<T> clazz, int max, Comparator<T> comparator) {
		this.comparator = comparator;
		ts = New.array(clazz, max + 1);
		size = 0;
	}

	public int add(T t) {
		int i, p;
		ts[++size] = t;
		for (i = size; 1 < i && 0 < compare(ts[p = i / 2], ts[i]); i = p)
			swap(p, i);
		return i;
	}

	public T extractMin() {
		return remove(1);
	}

	public T get(int i) {
		return ts[i];
	}

	public boolean isEmpty() {
		return size == 0;
	}

	public T min() {
		return 0 < size ? get(1) : null;
	}

	public T remove(int i) {
		int c;
		var t = ts[i];
		ts[i] = ts[size--];
		for (; (c = 2 * i) <= size; i = c) {
			if (c + 1 <= size && compare(ts[c + 1], ts[c]) < 0)
				c++;
			if (0 < compare(ts[i], ts[c]))
				swap(c, i);
			else
				break;
		}
		return t;
	}

	public int size() {
		return size;
	}

	private int compare(T t0, T t1) {
		return comparator.compare(t0, t1);
	}

	private void swap(int i0, int i1) {
		var temp = ts[i0];
		ts[i0] = ts[i1];
		ts[i1] = temp;
	}

}
