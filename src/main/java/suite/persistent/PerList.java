package suite.persistent;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Iterator;
import java.util.Objects;

import primal.Verbs.Build;
import primal.Verbs.Equals;
import primal.Verbs.Get;
import primal.fp.FunUtil;
import primal.fp.Funs.Source;
import primal.puller.Puller;
import primal.streamlet.Streamlet;

/**
 * Persistent linked list.
 *
 * @author ywsing
 */
public class PerList<T> implements Iterable<T> {

	public final T head;
	public final PerList<T> tail;

	private static PerList<?> end = new PerList<>(null, null);

	public static <T> PerList<T> end() {
		@SuppressWarnings("unchecked")
		var end = (PerList<T>) PerList.end;
		return end;
	}

	@SafeVarargs
	public static <T> PerList<T> of(T... ts) {
		var list = PerList.<T> end();
		for (var t : ts)
			list = cons(t, list);
		return list;
	}

	public static <T> PerList<T> cons(T t, PerList<T> list) {
		return new PerList<>(t, list);
	}

	public PerList(T head, PerList<T> tail) {
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
			private PerList<T> current = PerList.this;

			public T g() {
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

	public PerList<T> remove(T t) {
		PerList<T> result = end();
		for (var t_ : reverse())
			if (!Equals.ab(t, t_))
				result = PerList.cons(t_, result);
		return result;
	}

	public Deque<T> reverse() {
		var deque = new ArrayDeque<T>();
		for (var t : this)
			deque.addFirst(t);
		return deque;
	}

	public Streamlet<T> streamlet() {
		return new Streamlet<>(() -> Puller.of(new Source<T>() {
			private PerList<T> list = PerList.this;

			public T g() {
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
		if (Get.clazz(object) == PerList.class) {
			var list0 = this;
			var list1 = (PerList<?>) object;
			boolean e0, e1;
			while (true) {
				e0 = list0.isEmpty();
				e1 = list1.isEmpty();
				if (!e0 && !e1 && Equals.ab(list0.head, list1.head)) {
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
		return Build.string(sb -> {
			var node = this;
			while (!node.isEmpty()) {
				sb.append(node.head + ", ");
				node = node.tail;
			}
		});
	}

}
