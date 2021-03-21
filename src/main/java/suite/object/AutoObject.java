package suite.object;

import static primal.statics.Rethrow.ex;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import primal.Verbs.New;
import primal.adt.IdentityKey;
import primal.adt.Pair;
import primal.streamlet.Streamlet;
import suite.inspect.Inspect;
import suite.node.util.Singleton;

public class AutoObject<T extends AutoObject<T>> extends BaseObject<T> implements Cloneable, CastDefaults<T> {

	private static Inspect inspect = Singleton.me.inspect;

	@Override
	public T clone() {
		var map = new HashMap<IdentityKey<?>, AutoObject<?>>();

		return ex(() -> {
			@SuppressWarnings("unchecked")
			var object = (T) new Object() {
				private Object c_(Object v0) throws IllegalAccessException {
					if (v0 instanceof AutoObject<?> v1) {
						var key = IdentityKey.of(v1);
						var vx = map.get(key);
						if (vx == null) {
							map.put(key, vx = New.clazz(v1.getClass()));
							for (var field : v1.fields())
								field.set(vx, c_(field.get(v1)));
						}
						return vx;
					} else if (v0 instanceof Collection<?> col) {
						var v1 = new ArrayList<Object>();
						for (var c : col)
							v1.add(c_(c));
						return v1;
					} else if (v0 instanceof Map<?, ?> map) {
						var v1 = new HashMap<Object, Object>();
						for (var e : map.entrySet())
							v1.put(c_(e.getKey()), c_(e.getValue()));
						return v1;
					} else if (v0 instanceof Pair<?, ?> pair)
						return Pair.of(c_(pair.k), c_(pair.v));
					else
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
