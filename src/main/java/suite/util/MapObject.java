package suite.util;

import java.util.Objects;

import suite.immutable.IList;

public abstract class MapObject<T extends MapObject<T>> implements Cloneable, Comparable<T> {

	private static ThreadLocal<IList<MapObject<?>>> recurse = ThreadLocal.withInitial(IList::end);

	@Override
	public MapObject<T> clone() {
		return MapObject_.construct(getClass(), MapObject_.list(this));
	}

	@Override
	public int compareTo(T other) {
		return MapObject_.compare(self(), other);
	}

	@Override
	public boolean equals(Object object) {
		T t0 = self();
		boolean b;
		if (t0.getClass() == object.getClass()) {
			@SuppressWarnings("unchecked")
			T t1 = (T) object;
			b = MapObject_.equals(t0, t1);
		} else
			b = false;
		return b;
	}

	@Override
	public int hashCode() {
		int hashCode = 5;
		for (Object value : MapObject_.list(this))
			hashCode = 31 * hashCode + Objects.hashCode(value);
		return hashCode;
	}

	@Override
	public String toString() {
		IList<MapObject<?>> recurse0 = recurse.get();
		StringBuilder sb = new StringBuilder();

		if (!recurse0.contains(this))
			try {
				sb.append(getClass().getSimpleName() + "(");
				for (Object value : MapObject_.list(this))
					sb.append(value + ",");
				sb.append(")");
				return sb.toString();
			} finally {
				recurse.set(recurse0);
			}
		else
			return "<recurse>";
	}

	private T self() {
		@SuppressWarnings("unchecked")
		T t = (T) this;
		return t;
	}

}
