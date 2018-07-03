package suite.immutable;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Iterator;
import java.util.Objects;

import suite.object.Object_;
import suite.streamlet.FunUtil;
import suite.streamlet.FunUtil.Source;
import suite.streamlet.Outlet;
import suite.streamlet.Streamlet;

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
		var end = (IList<T>) IList.end;
		return end;
	}

	@SafeVarargs
	public static <T> IList<T> of(T... ts) {
		var list = IList.<T> end();
		for (var t : ts)
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

	public boolean containsId(T t) {
		return !isEmpty() ? head == t || tail.containsId(t) : false;
	}

	public boolean isEmpty() {
		return this == end;
	}

	@Override
	public Iterator<T> iterator() {
		return FunUtil.iterator(new Source<>() {
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

	public IList<T> remove(T t) {
		IList<T> result = end();
		for (var t_ : reverse())
			if (!Objects.equals(t, t_))
				result = IList.cons(t_, result);
		return result;
	}

	public Deque<T> reverse() {
		var deque = new ArrayDeque<T>();
		for (var t : this)
			deque.addFirst(t);
		return deque;
	}

	public Streamlet<T> streamlet() {
		return new Streamlet<>(() -> Outlet.of(new Source<T>() {
			private IList<T> list = IList.this;

			public T source() {
				if (list != null) {
					var t = list.head;
					list = list.tail;
					return t;
				} else
					return null;
			}
		}));
	}

	@Override
	public boolean equals(Object object) {
		if (Object_.clazz(object) == IList.class) {
			var list0 = this;
			var list1 = (IList<?>) object;
			boolean e0, e1;
			while (true) {
				e0 = list0.isEmpty();
				e1 = list1.isEmpty();
				if (!e0 && !e1 && Objects.equals(list0.head, list1.head)) {
					list0 = list0.tail;
					list1 = list1.tail;
				} else
					break;
			}
			return e0 && e1;
		} else
			return false;
	}

	@Override
	public int hashCode() {
		var list = this;
		var h = 7;
		while (!list.isEmpty()) {
			h = h * 31 + Objects.hashCode(list.head);
			list = list.tail;
		}
		return h;
	}

	@Override
	public String toString() {
		var sb = new StringBuilder();
		var node = this;
		while (!node.isEmpty()) {
			sb.append(node.head + ", ");
			node = node.tail;
		}
		return sb.toString();
	}

}
