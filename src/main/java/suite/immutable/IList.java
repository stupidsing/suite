package suite.immutable;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Iterator;

import suite.util.FunUtil;
import suite.util.FunUtil.Source;

/**
 * Immutable linked list.
 *
 * @author ywsing
 */
public class IList<T> implements Iterable<T> {

	public final T head;
	public final IList<T> tail;

	private static IList<?> end = new IList<>(null, null);

	public static <T> IList<T> end() {
		@SuppressWarnings("unchecked")
		IList<T> end = (IList<T>) IList.end;
		return end;
	}

	@SafeVarargs
	public static <T> IList<T> asList(T... ts) {
		IList<T> list = IList.<T> end();
		for (T t : ts)
			list = cons(t, list);
		return list;
	}

	public static <T> IList<T> cons(T t, IList<T> list) {
		return new IList<>(t, list);
	}

	public IList(T head, IList<T> tail) {
		this.head = head;
		this.tail = tail;
	}

	public boolean contains(T t) {
		return !isEmpty() ? head.equals(t) || tail.contains(t) : false;
	}

	public boolean isEmpty() {
		return this == end;
	}

	@Override
	public Iterator<T> iterator() {
		return FunUtil.iterator(new Source<T>() {
			private IList<T> current = IList.this;

			public T source() {
				T t;
				if (current != end) {
					t = current.head;
					current = current.tail;
				} else
					t = null;
				return t;
			}
		});
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		IList<T> node = this;
		while (!node.isEmpty()) {
			sb.append(node.head + ", ");
			node = node.tail;
		}
		return sb.toString();
	}

	public Deque<T> reverse() {
		Deque<T> deque = new ArrayDeque<>();
		for (T t : this)
			deque.addFirst(t);
		return deque;
	}

}
