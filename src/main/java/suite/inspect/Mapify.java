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
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import suite.jdk.gen.Type_;
import suite.streamlet.Read;
import suite.util.Fail;
import suite.util.FunUtil.Iterate;
import suite.util.Object_;
import suite.util.Rethrow;
import suite.util.To;

/**
 * Convert (supposedly) any Java structures to recursive maps.
 *
 * @author ywsing
 */
public class Mapify {

	private Set<Type> collectionClasses = To.set(ArrayList.class, Collection.class, HashSet.class, List.class, Set.class);
	private Set<Type> mapClasses = To.set(HashMap.class, Map.class);

	private Iterate<Object> id = object -> object;

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
					object -> apply_(getMapifier(type).mapify, object), //
					object -> apply_(getMapifier(type).unmapify, object)));
			mapifiers.put(type, mapifier = newMapifier(type));
		}
		return mapifier;
	}

	@SuppressWarnings("unchecked")
	private Mapifier newMapifier(Type type) {
		Mapifier mapifier;

		if (type instanceof Class) {
			Class<?> clazz = (Class<?>) type;

			if (Type_.isSimple(clazz))
				mapifier = new Mapifier(id, id);
			else if (clazz.isArray()) {
				Class<?> componentType = clazz.getComponentType();
				var mapifier1 = getMapifier(componentType);
				mapifier = new Mapifier(object -> {
					var map = newMap();
					var length = Array.getLength(object);
					for (var i = 0; i < length; i++)
						map.put(i, apply_(mapifier1.mapify, Array.get(object, i)));
					return map;
				}, object -> {
					Map<?, ?> map = (Map<?, ?>) object;
					Object objects = Array.newInstance(componentType, map.size());
					var i = 0;
					while (map.containsKey(i)) {
						Array.set(objects, i, apply_(mapifier1.unmapify, map.get(i)));
						i++;
					}
					return objects;
				});
			} else if (clazz.isInterface() || Modifier.isAbstract(clazz.getModifiers())) // polymorphism
				mapifier = new Mapifier(object -> {
					Class<?> clazz1 = object.getClass();
					Object m = apply_(getMapifier(clazz1).mapify, object);
					if (m instanceof Map) {
						var map = (Map<String, String>) m;
						map.put("@class", clazz1.getName());
						return map;
					} else
						// happens when an enum implements an interface
						return m;
				}, object -> {
					if (object instanceof Map) {
						Map<?, ?> map = (Map<?, ?>) object;
						var className = map.get("@class").toString();
						Class<?> clazz1 = Rethrow.ex(() -> Class.forName(className));
						return apply_(getMapifier(clazz1).unmapify, object);
					} else
						// happens when an enum implements an interface
						return object;
				});
			else {
				var fis = Read //
						.from(inspect.fields(clazz)) //
						.map(field -> {
							var type1 = field.getGenericType();
							return new FieldInfo(field, field.getName(), getMapifier(type1));
						}) //
						.toList();

				mapifier = new Mapifier(object -> Rethrow.ex(() -> {
					var map = newMap();
					for (var fi : fis)
						map.put(fi.name, apply_(fi.mapifier.mapify, fi.field.get(object)));
					return map;
				}), object -> Rethrow.ex(() -> {
					Map<?, ?> map = (Map<?, ?>) object;
					var object1 = Object_.new_(clazz);
					for (var fi : fis)
						fi.field.set(object1, apply_(fi.mapifier.unmapify, map.get(fi.name)));
					return object1;
				}));
			}
		} else if (type instanceof ParameterizedType) {
			var pt = (ParameterizedType) type;
			var rawType = pt.getRawType();
			Type[] typeArgs = pt.getActualTypeArguments();
			Class<?> clazz = rawType instanceof Class ? (Class<?>) rawType : null;

			if (collectionClasses.contains(clazz)) {
				var mapifier1 = getMapifier(typeArgs[0]);
				mapifier = new Mapifier(object -> {
					var map = newMap();
					var i = 0;
					for (var o : (Collection<?>) object)
						map.put(i++, apply_(mapifier1.mapify, o));
					return map;
				}, object -> {
					Map<?, ?> map = (Map<?, ?>) object;
					Collection<Object> object1 = (Collection<Object>) instantiate(clazz);
					var i = 0;
					while (map.containsKey(i))
						object1.add(apply_(mapifier1.unmapify, map.get(i++)));
					return object1;
				});
			} else if (mapClasses.contains(clazz)) {
				var km = getMapifier(typeArgs[0]);
				var vm = getMapifier(typeArgs[1]);
				mapifier = new Mapifier(object -> {
					var map = newMap();
					for (Entry<?, ?> e : ((Map<?, ?>) object).entrySet())
						map.put(apply_(km.mapify, e.getKey()), apply_(vm.mapify, e.getValue()));
					return map;
				}, object -> {
					Map<?, ?> map = (Map<?, ?>) object;
					var object1 = (Map<Object, Object>) instantiate(clazz);
					for (Entry<?, ?> e : map.entrySet())
						object1.put(apply_(km.unmapify, e.getKey()), apply_(vm.unmapify, e.getValue()));
					return object1;
				});
			} else
				mapifier = getMapifier(rawType);
		} else
			mapifier = Fail.t("unrecognized type " + type);

		return mapifier;
	}

	private <T> T instantiate(Class<T> clazz) {
		Object object;
		if (clazz == ArrayList.class || clazz == Collection.class || clazz == List.class)
			object = new ArrayList<>();
		else if (clazz == HashSet.class || clazz == Set.class)
			object = new HashSet<>();
		else if (clazz == HashMap.class || clazz == Map.class)
			object = new HashMap<>();
		else
			return Object_.new_(clazz);

		@SuppressWarnings("unchecked")
		var t = (T) object;
		return t;
	}

	private Object apply_(Iterate<Object> fun, Object object) {
		return object != null ? fun.apply(object) : null;
	}

	private Map<Object, Object> newMap() {
		return new HashMap<>();
	}

}
