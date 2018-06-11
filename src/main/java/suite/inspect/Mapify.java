package suite.inspect;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import suite.primitive.Ints_;
import suite.streamlet.Read;
import suite.util.FunUtil.Iterate;
import suite.util.Object_;
import suite.util.Rethrow;
import suite.util.Switch;
import suite.util.To;
import suite.util.Util;

/**
 * Convert (supposedly) any Java structures to recursive maps.
 *
 * @author ywsing
 */
public class Mapify {

	private Set<Type> collectionClasses = Set.of(ArrayList.class, Collection.class, HashSet.class, List.class, Set.class);
	private Set<Type> mapClasses = Set.of(HashMap.class, Map.class);

	private Iterate<Object> id = o -> o;

	private Map<Type, Mapifier> mapifiers = new ConcurrentHashMap<>();

	private Inspect inspect;

	private class FieldInfo {
		private Field field;
		private String name;
		private Mapifier mapifier;

		private FieldInfo(Field field, String name, Mapifier mapifier) {
			this.field = field;
			this.name = name;
			this.mapifier = mapifier;
		}
	}

	private class Mapifier {
		private Iterate<Object> mapify;
		private Iterate<Object> unmapify;

		private Mapifier(Iterate<Object> mapify, Iterate<Object> unmapify) {
			this.mapify = mapify;
			this.unmapify = unmapify;
		}
	}

	public Mapify(Inspect inspect) {
		this.inspect = inspect;
	}

	public <T> Object mapify(Class<T> clazz, T t) {
		return apply_(getMapifier(clazz).mapify, t);
	}

	public <T> T unmapify(Class<T> clazz, Object object) {
		@SuppressWarnings("unchecked")
		T t = (T) apply_(getMapifier(clazz).unmapify, object);
		return t;
	}

	private Mapifier getMapifier(Type type) {
		var mapifier = mapifiers.get(type);
		if (mapifier == null) {
			mapifiers.put(type, new Mapifier( //
					o -> apply_(getMapifier(type).mapify, o), //
					o -> apply_(getMapifier(type).unmapify, o)));
			mapifiers.put(type, mapifier = newMapifier(type));
		}
		return mapifier;
	}

	@SuppressWarnings("unchecked")
	private Mapifier newMapifier(Type type) {
		return new Switch<Mapifier>(type //
		).applyIf(Class.class, clazz -> {
			if (Util.isSimple(clazz))
				return new Mapifier(id, id);
			else if (clazz.isArray()) {
				var componentType = clazz.getComponentType();
				var mapifier1 = getMapifier(componentType);
				return new Mapifier(o -> {
					return Ints_.range(Array.getLength(o)).map2(i -> i, i -> apply_(mapifier1.mapify, Array.get(o, i))).toMap();
				}, o -> {
					var map = (Map<?, ?>) o;
					return To.array_(map.size(), componentType, i -> apply_(mapifier1.unmapify, map.get(i)));
				});
			} else if (clazz.isInterface() || Modifier.isAbstract(clazz.getModifiers())) // polymorphism
				return new Mapifier(o -> {
					var clazz1 = o.getClass();
					var m = apply_(getMapifier(clazz1).mapify, o);
					if (m instanceof Map) {
						var map = (Map<String, String>) m;
						map.put("@class", clazz1.getName());
						return map;
					} else
						// happens when an enum implements an interface
						return m;
				}, o -> {
					if (o instanceof Map) {
						var map = (Map<?, ?>) o;
						var className = map.get("@class").toString();
						var clazz1 = Rethrow.ex(() -> Class.forName(className));
						return apply_(getMapifier(clazz1).unmapify, o);
					} else
						// happens when an enum implements an interface
						return o;
				});
			else {
				var fis = Read //
						.from(inspect.fields(clazz)) //
						.map(field -> new FieldInfo(field, field.getName(), getMapifier(field.getGenericType()))) //
						.toList();

				return new Mapifier(o -> {
					return Read //
							.from(fis) //
							.map2(fi -> fi.name, fi -> apply_(fi.mapifier.mapify, Rethrow.ex(() -> fi.field.get(o)))) //
							.toMap();
				}, o -> Rethrow.ex(() -> {
					var map = (Map<?, ?>) o;
					var object1 = Object_.new_(clazz);
					for (var fi : fis)
						fi.field.set(object1, apply_(fi.mapifier.unmapify, map.get(fi.name)));
					return object1;
				}));
			}
		}).applyIf(ParameterizedType.class, pt -> {
			var rawType = pt.getRawType();
			var typeArgs = pt.getActualTypeArguments();
			var clazz = rawType instanceof Class ? (Class<?>) rawType : null;

			if (collectionClasses.contains(clazz)) {
				var mapifier1 = getMapifier(typeArgs[0]);
				return new Mapifier(o -> {
					var map = newMap();
					var i = 0;
					for (var o_ : (Collection<?>) o)
						map.put(i++, apply_(mapifier1.mapify, o_));
					return map;
				}, o -> {
					var map = (Map<?, ?>) o;
					var object1 = (Collection<Object>) Object_.instantiate(clazz);
					var i = 0;
					while (map.containsKey(i))
						object1.add(apply_(mapifier1.unmapify, map.get(i++)));
					return object1;
				});
			} else if (mapClasses.contains(clazz)) {
				var km = getMapifier(typeArgs[0]);
				var vm = getMapifier(typeArgs[1]);
				return new Mapifier(o -> {
					return Read //
							.from2((Map<?, ?>) o) //
							.map2((k, v) -> apply_(km.unmapify, k), (k, v) -> apply_(vm.mapify, v)) //
							.toMap();
				}, o -> {
					var object1 = (Map<Object, Object>) Object_.instantiate(clazz);
					((Map<?, ?>) o).forEach((k, v) -> object1.put(apply_(km.unmapify, k), apply_(vm.unmapify, v)));
					return object1;
				});
			} else
				return getMapifier(rawType);
		}).nonNullResult();
	}

	private Object apply_(Iterate<Object> fun, Object object) {
		return object != null ? fun.apply(object) : null;
	}

	private Map<Object, Object> newMap() {
		return new HashMap<>();
	}

}
