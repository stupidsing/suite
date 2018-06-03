package suite.util;

import java.util.HashMap;
import java.util.List;

import suite.adt.IdentityKey;
import suite.adt.pair.Fixie_.FixieFun0;
import suite.adt.pair.Fixie_.FixieFun1;
import suite.adt.pair.Fixie_.FixieFun2;
import suite.adt.pair.Fixie_.FixieFun3;
import suite.adt.pair.Fixie_.FixieFun4;
import suite.adt.pair.Fixie_.FixieFun5;
import suite.adt.pair.Fixie_.FixieFun6;
import suite.adt.pair.Fixie_.FixieFun7;
import suite.adt.pair.Fixie_.FixieFun8;
import suite.adt.pair.Fixie_.FixieFun9;
import suite.adt.pair.Fixie_.FixieFunA;
import suite.streamlet.Read;

public class MapObject_ {

	public static <T extends MapObject<T>> T clone(T t) {
		var map = new HashMap<IdentityKey<?>, MapObject<?>>();

		class Clone {
			private MapObject<?> clone(MapObject<?> t0) throws IllegalAccessException {
				var key = IdentityKey.of(t0);
				var tx = map.get(key);
				if (tx == null) {
					var list0 = Read.from(list(t0));
					var list1 = list0.map(v -> v instanceof MapObject ? ((MapObject<?>) v).clone() : v).toList();
					map.put(key, tx = construct(getClass(), list1));
				}
				return tx;
			}
		}

		return Rethrow.ex(() -> {
			@SuppressWarnings("unchecked")
			var object = (T) new Clone().clone(t);
			return object;
		});
	}

	public static <T extends MapObject<T>> MapObject<T> construct(Class<?> clazz, List<?> list) {
		return Rethrow.ex(() -> {
			var size = list.size();
			var m = Read //
					.from(clazz.getMethods()) //
					.filter(method -> String_.equals(method.getName(), "of") && method.getParameterCount() == size) //
					.uniqueResult();
			@SuppressWarnings("unchecked")
			var t = (MapObject<T>) m.invoke(null, list.toArray());
			return t;
		});
	}

	public static <T extends MapObject<T>> List<?> list(Object object) {
		var clazz = object.getClass();

		var m = Read //
				.from(clazz.getMethods()) //
				.filter(method -> String_.equals(method.getName(), "apply")) //
				.uniqueResult();

		var type = m.getParameters()[0].getType();
		Object p;

		if (type == FixieFun0.class)
			p = (FixieFun0<?>) List::of;
		else if (type == FixieFun1.class)
			p = (FixieFun1<?, ?>) List::of;
		else if (type == FixieFun2.class)
			p = (FixieFun2<?, ?, ?>) List::of;
		else if (type == FixieFun3.class)
			p = (FixieFun3<?, ?, ?, ?>) List::of;
		else if (type == FixieFun4.class)
			p = (FixieFun4<?, ?, ?, ?, ?>) List::of;
		else if (type == FixieFun5.class)
			p = (FixieFun5<?, ?, ?, ?, ?, ?>) List::of;
		else if (type == FixieFun6.class)
			p = (FixieFun6<?, ?, ?, ?, ?, ?, ?>) List::of;
		else if (type == FixieFun7.class)
			p = (FixieFun7<?, ?, ?, ?, ?, ?, ?, ?>) List::of;
		else if (type == FixieFun8.class)
			p = (FixieFun8<?, ?, ?, ?, ?, ?, ?, ?, ?>) List::of;
		else if (type == FixieFun9.class)
			p = (FixieFun9<?, ?, ?, ?, ?, ?, ?, ?, ?, ?>) List::of;
		else if (type == FixieFunA.class)
			p = (FixieFunA<?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?>) List::of;
		else
			p = Fail.t();

		return (List<?>) Rethrow.ex(() -> m.invoke(object, p));
	}

}
