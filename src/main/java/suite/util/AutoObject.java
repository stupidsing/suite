package suite.util;

import java.util.Iterator;
import java.util.List;
import java.util.Objects;

import suite.inspect.Inspect;
import suite.node.util.Singleton;
import suite.streamlet.Read;

public abstract class AutoObject<T extends AutoObject<T>> implements Comparable<T> {

	private static Inspect inspect = Singleton.get().getInspect();

	@Override
	public int compareTo(T t1) {
		Class<?> class0 = getClass();
		Class<?> class1 = t1.getClass();
		int c;
		if (class0 == class1) {
			T t0 = self();
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
			T t0 = self();
			@SuppressWarnings("unchecked")
			T t1 = (T) object;
			List<Comparable<?>> values0 = t0.values();
			List<Comparable<?>> values1 = t1.values();
			int size0 = values0.size();
			int size1 = values1.size();
			b = true;
			if (size0 == size1)
				for (int i = 0; i < size0; i++)
					b &= Objects.equals(values0.get(i), values1.get(i));
		} else
			b = false;
		return b;
	}

	@Override
	public int hashCode() {
		int hashCode = 5;
		for (Comparable<?> value : values())
			hashCode = 31 * hashCode + Objects.hashCode(value);
		return hashCode;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append(getClass().getSimpleName() + "(");
		for (Object value : values())
			sb.append(value + ", ");
		sb.deleteCharAt(sb.length() - 1);
		sb.append(")");
		return sb.toString();
	}

	public List<Comparable<?>> values() {
		List<?> list0 = Read //
				.from(inspect.fields(getClass())) //
				.map(field -> Rethrow.ex(() -> field.get(this))) //
				.toList();
		@SuppressWarnings("unchecked")
		List<Comparable<?>> list1 = (List<Comparable<?>>) list0;
		return list1;
	}

	private T self() {
		@SuppressWarnings("unchecked")
		T t = (T) this;
		return t;
	}

}
