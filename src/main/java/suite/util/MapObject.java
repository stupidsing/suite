package suite.util;

import java.util.HashMap;

import suite.adt.IdentityKey;
import suite.streamlet.Read;

public abstract class MapObject<T extends MapObject<T>> implements Cloneable, Comparable<T>, MapInterface<T> {

	@Override
	public MapObject<T> clone() {
		var map = new HashMap<IdentityKey<?>, MapObject<?>>();

		return Rethrow.ex(() -> {
			@SuppressWarnings("unchecked")
			T object = (T) new Object() {
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
			}.clone(this);

			return object;
		});
	}

	@Override
	public int compareTo(T other) {
		return autoObject().compare(self(), other);
	}

	@Override
	public boolean equals(Object object) {
		return autoObject().isEquals(self(), object);
	}

	@Override
	public int hashCode() {
		return autoObject().hashCode(self());
	}

	@Override
	public String toString() {
		return autoObject().toString(self());
	}

	private AutoObject_<Object> autoObject() {
		return new AutoObject_<>(MapObject_::list);
	}

	private T self() {
		@SuppressWarnings("unchecked")
		var t = (T) this;
		return t;
	}

}
