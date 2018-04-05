package suite.util;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import suite.adt.IdentityKey;
import suite.immutable.IList;
import suite.inspect.Inspect;
import suite.node.util.Singleton;
import suite.streamlet.Read;
import suite.streamlet.Streamlet;

public abstract class AutoObject<T extends AutoObject<T>> implements Cloneable, Comparable<T> {

	private static Inspect inspect = Singleton.me.inspect;
	private static ThreadLocal<IList<AutoObject<?>>> recurse = ThreadLocal.withInitial(IList::end);

	@Override
	public AutoObject<T> clone() {
		Map<IdentityKey<?>, AutoObject<?>> map = new HashMap<>();

		class Clone {
			private AutoObject<?> clone(AutoObject<?> t0) throws IllegalAccessException {
				IdentityKey<?> key = IdentityKey.of(t0);
				AutoObject<?> tx = map.get(key);
				if (tx == null) {
					map.put(key, tx = Object_.new_(t0.getClass()));
					@SuppressWarnings("unchecked")
					AutoObject<T> t1 = (AutoObject<T>) tx;
					for (var field : t0.fields_()) {
						var v0 = field.get(t0);
						var v1 = v0 instanceof AutoObject ? clone((AutoObject<?>) v0) : v0;
						field.set(t1, v1);
					}
				}
				return tx;
			}
		}

		return Rethrow.ex(() -> {
			@SuppressWarnings("unchecked")
			AutoObject<T> object = (AutoObject<T>) new Clone().clone(this);
			return object;
		});
	}

	@Override
	public int compareTo(T t1) {
		Class<?> class0 = getClass();
		Class<?> class1 = t1.getClass();
		int c;
		if (class0 == class1) {
			var t0 = self();
			Iterator<Comparable<?>> iter0 = t0.values().iterator();
			Iterator<Comparable<?>> iter1 = t1.values().iterator();
			boolean b0, b1;
			c = 0;
			while (c == 0 && (c = Boolean.compare(b0 = iter0.hasNext(), b1 = iter1.hasNext())) == 0)
				if (b0 && b1) {
					@SuppressWarnings("unchecked")
					Comparable<Object> value0 = (Comparable<Object>) iter0.next();
					@SuppressWarnings("unchecked")
					Comparable<Object> value1 = (Comparable<Object>) iter1.next();
					c = value0.compareTo(value1);
				}
		} else
			c = Object_.compare(class0.getName(), class1.getName());
		return c;
	}

	@Override
	public boolean equals(Object object) {
		boolean b;
		if (getClass() == object.getClass()) {
			var t0 = self();
			@SuppressWarnings("unchecked")
			var t1 = (T) object;
			List<Comparable<?>> values0 = t0.values();
			List<Comparable<?>> values1 = t1.values();
			var size0 = values0.size();
			var size1 = values1.size();
			b = true;
			if (size0 == size1)
				for (var i = 0; i < size0; i++)
					b &= Objects.equals(values0.get(i), values1.get(i));
		} else
			b = false;
		return b;
	}

	public Streamlet<Field> fields() {
		return fields_();
	}

	@Override
	public int hashCode() {
		var h = 7;
		for (var value : values())
			h = h * 31 + Objects.hashCode(value);
		return h;
	}

	@Override
	public String toString() {
		IList<AutoObject<?>> recurse0 = recurse.get();
		var sb = new StringBuilder();

		if (!recurse0.contains(this))
			try {
				recurse.set(IList.cons(this, recurse0));
				sb.append(getClass().getSimpleName() + "(");
				for (var value : values())
					sb.append(value + ",");
				sb.append(")");
				return sb.toString();
			} finally {
				recurse.set(recurse0);
			}
		else
			return "<recurse>";
	}

	public List<Comparable<?>> values() {
		List<?> list0 = fields_() //
				.map(field -> Rethrow.ex(() -> field.get(this))) //
				.toList();
		@SuppressWarnings("unchecked")
		List<Comparable<?>> list1 = (List<Comparable<?>>) list0;
		return list1;
	}

	private Streamlet<Field> fields_() {
		return Read.from(inspect.fields(getClass()));
	}

	private T self() {
		@SuppressWarnings("unchecked")
		var t = (T) this;
		return t;
	}

}
