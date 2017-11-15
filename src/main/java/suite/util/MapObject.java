package suite.util;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import suite.adt.IdentityKey;
import suite.immutable.IList;
import suite.streamlet.Read;
import suite.streamlet.Streamlet;

public abstract class MapObject<T extends MapObject<T>> implements Cloneable, Comparable<T> {

	private static ThreadLocal<IList<MapObject<?>>> recurse = ThreadLocal.withInitial(IList::end);

	@Override
	public MapObject<T> clone() {
		Map<IdentityKey<?>, MapObject<?>> map = new HashMap<>();

		class Clone {
			private MapObject<?> clone(MapObject<?> t0) throws IllegalAccessException {
				IdentityKey<?> key = IdentityKey.of(t0);
				MapObject<?> tx = map.get(key);
				if (tx == null) {
					Streamlet<?> list0 = Read.from(MapObject_.list(t0));
					List<Object> list1 = list0.map(v -> v instanceof MapObject ? ((MapObject<?>) v).clone() : v).toList();
					map.put(key, tx = MapObject_.construct(getClass(), list1));
				}
				return tx;
			}
		}

		return Rethrow.ex(() -> {
			@SuppressWarnings("unchecked")
			MapObject<T> object = (MapObject<T>) new Clone().clone(this);
			return object;
		});
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
