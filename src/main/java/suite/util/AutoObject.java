package suite.util;

import static suite.util.Friends.rethrow;

import java.lang.reflect.Field;
import java.util.HashMap;

import suite.adt.IdentityKey;
import suite.inspect.Inspect;
import suite.node.util.Singleton;
import suite.streamlet.Streamlet;

public class AutoObject<T extends AutoObject<T>> extends BaseObject<T> implements Cloneable, AutoInterface<T> {

	private static Inspect inspect = Singleton.me.inspect;

	@Override
	public T clone() {
		var map = new HashMap<IdentityKey<?>, AutoObject<?>>();

		return rethrow(() -> {
			@SuppressWarnings("unchecked")
			var object = (T) new Object() {
				private AutoObject<?> clone(AutoObject<?> t0) throws IllegalAccessException {
					var key = IdentityKey.of(t0);
					var tx = map.get(key);
					if (tx == null) {
						map.put(key, tx = Object_.new_(t0.getClass()));
						var t1 = (AutoObject<T>) tx;
						for (var field : t0.fields()) {
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

	public Streamlet<Field> fields() {
		return inspect.fields(getClass());
	}

	@Override
	protected ObjectSupport<T> objectSupport() {
		return new ObjectSupport<>(inspect::values);
	}

}
