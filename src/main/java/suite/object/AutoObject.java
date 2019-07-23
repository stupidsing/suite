package suite.object;

import static suite.util.Friends.rethrow;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;

import suite.adt.IdentityKey;
import suite.adt.pair.Pair;
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
				private Object c_(Object v0) throws IllegalAccessException {
					if (v0 instanceof AutoObject) {
						var v1 = (AutoObject<?>) v0;
						var key = IdentityKey.of(v1);
						var vx = map.get(key);
						if (vx == null) {
							map.put(key, vx = Object_.new_(v1.getClass()));
							for (var field : v1.fields())
								field.set(vx, c_(field.get(v1)));
						}
						return vx;
					} else if (v0 instanceof Collection) {
						var v1 = new ArrayList<Object>();
						for (var c : (Collection<?>) v0)
							v1.add(c_(c));
						return v1;
					} else if (v0 instanceof Pair) {
						var pair = (Pair<?, ?>) v0;
						return Pair.of(c_(pair.k), c_(pair.v));
					} else
						return v0;
				}
			}.c_(this);

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
