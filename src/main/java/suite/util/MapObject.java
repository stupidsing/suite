package suite.util;

import java.util.HashMap;
import java.util.Objects;

import suite.adt.IdentityKey;
import suite.immutable.IList;
import suite.streamlet.Read;

public abstract class MapObject<T extends MapObject<T>> implements Cloneable, Comparable<T>, MapInterface<T> {

	private static ThreadLocal<IList<MapObject<?>>> recurse = ThreadLocal.withInitial(IList::end);

	@Override
	public MapObject<T> clone() {
		var map = new HashMap<IdentityKey<?>, MapObject<?>>();

		class Clone {
			private MapObject<?> clone(MapObject<?> t0) throws IllegalAccessException {
				var key = IdentityKey.of(t0);
				var tx = map.get(key);
				if (tx == null) {
					var list0 = Read.from(MapObject_.list(t0));
					var list1 = list0.map(v -> v instanceof MapObject ? ((MapObject<?>) v).clone() : v).toList();
					map.put(key, tx = MapObject_.construct(getClass(), list1));
				}
				return tx;
			}
		}

		return Rethrow.ex(() -> {
			@SuppressWarnings("unchecked")
			var object = (MapObject<T>) new Clone().clone(this);
			return object;
		});
	}

	@Override
	public int compareTo(T other) {
		return MapObject_.compare(self(), other);
	}

	@Override
	public boolean equals(Object object) {
		var t0 = self();
		boolean b;
		if (t0.getClass() == object.getClass()) {
			@SuppressWarnings("unchecked")
			var t1 = (T) object;
			b = MapObject_.equals(t0, t1);
		} else
			b = false;
		return b;
	}

	@Override
	public int hashCode() {
		var h = 7;
		for (var value : MapObject_.list(this))
			h = h * 31 + Objects.hashCode(value);
		return h;
	}

	@Override
	public String toString() {
		var recurse0 = recurse.get();
		var sb = new StringBuilder();

		if (!recurse0.contains(this))
			try {
				sb.append(getClass().getSimpleName() + "(");
				for (var value : MapObject_.list(this))
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
		var t = (T) this;
		return t;
	}

}
