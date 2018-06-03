package suite.util;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import suite.adt.IdentityKey;
import suite.inspect.Inspect;
import suite.node.util.Singleton;
import suite.streamlet.Read;
import suite.streamlet.Streamlet;

public abstract class AutoObject<T extends AutoObject<T>> implements Cloneable, Comparable<T> {

	private static Inspect inspect = Singleton.me.inspect;

	@Override
	public AutoObject<T> clone() {
		return Rethrow.ex(() -> {
			@SuppressWarnings("unchecked")
			var object = (AutoObject<T>) new Object() {
				private Map<IdentityKey<?>, AutoObject<?>> map = new HashMap<>();

				private AutoObject<?> clone(AutoObject<?> t0) throws IllegalAccessException {
					var key = IdentityKey.of(t0);
					var tx = map.get(key);
					if (tx == null) {
						map.put(key, tx = Object_.new_(t0.getClass()));
						var t1 = (AutoObject<T>) tx;
						for (var field : t0.fields_()) {
							var v0 = field.get(t0);
							var v1 = v0 instanceof AutoObject ? clone((AutoObject<?>) v0) : v0;
							field.set(t1, v1);
						}
					}
					return tx;
				}
			}.clone(this);

			return object;
		});
	}

	@Override
	public int compareTo(T t1) {
		return new AutoObject_<>(T::values).compare(self(), t1);
	}

	@Override
	public boolean equals(Object object) {
		boolean b;
		if (getClass() == object.getClass()) {
			var t0 = self();
			@SuppressWarnings("unchecked")
			var t1 = (T) object;
			return new AutoObject_<>(T::values).equals(t0, t1);
		} else
			b = false;
		return b;
	}

	public Streamlet<Field> fields() {
		return fields_();
	}

	@Override
	public int hashCode() {
		return new AutoObject_<>(T::values).hashCode(self());
	}

	@Override
	public String toString() {
		return new AutoObject_<>(T::values).toString(self());
	}

	public List<?> values() {
		return fields_() //
				.map(field -> Rethrow.ex(() -> field.get(this))) //
				.toList();
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
