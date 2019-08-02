package suite.object;

import static primal.statics.Rethrow.ex;

import java.util.HashMap;

import primal.adt.IdentityKey;
import suite.streamlet.Read;

/**
 * An object having the following methods:
 * 
 * - a static Xxx.of(a, b, c) constructor
 * 
 * - a static apply(Fun<A, B, C, T> fun) mapper
 * 
 * @author ywsing
 */
public class MapObject<T extends MapObject<T>> extends BaseObject<T> implements Cloneable {

	@Override
	public T clone() {
		var map = new HashMap<IdentityKey<?>, MapObject<?>>();

		return ex(() -> {
			@SuppressWarnings("unchecked")
			var object = (T) new Object() {
				private MapObject<?> clone(MapObject<?> t0) {
					var key = IdentityKey.of(t0);
					var tx = map.get(key);
					if (tx == null) {
						var list0 = Read.from(MapObject_.list(t0));
						var list1 = list0.map(v -> v instanceof MapObject ? clone((MapObject<?>) v) : v).toList();
						map.put(key, tx = MapObject_.construct(getClass(), list1));
					}
					return tx;
				}
			}.clone(this);

			return object;
		});
	}

	@Override
	protected ObjectSupport<T> objectSupport() {
		return new ObjectSupport<>(MapObject_::list);
	}

}
