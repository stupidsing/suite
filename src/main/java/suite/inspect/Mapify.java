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

import suite.streamlet.Read;
import suite.util.FunUtil.Fun;
import suite.util.Rethrow;
import suite.util.Util;

/**
 * Convert (supposedly) any Java structures to recursive maps.
 *
 * @author ywsing
 */
public class Mapify {

	private Set<Type> collectionClasses = Util.set(ArrayList.class, Collection.class, HashSet.class, List.class, Set.class);
	private Set<Type> mapClasses = Util.set(HashMap.class, Map.class);

	private Fun<Object, Object> id = object -> object;

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
		private Fun<Object, Object> mapify;
		private Fun<Object, Object> unmapify;

		private Mapifier(Fun<Object, Object> mapify, Fun<Object, Object> unmapify) {
			this.mapify = mapify;
			this.unmapify = unmapify;
		}
	}

	public Mapify(Inspect inspect) {
		this.inspect = inspect;
	}

	public <T> Object mapify(Class<T> clazz, T t) {
		return apply0(getMapifier(clazz).mapify, t);
	}

	public <T> T unmapify(Class<T> clazz, Object object) {
		@SuppressWarnings("unchecked")
		T t = (T) apply0(getMapifier(clazz).unmapify, object);
		return t;
	}

	private Mapifier getMapifier(Type type) {
		Mapifier mapifier = mapifiers.get(type);
		if (mapifier == null) {
			mapifiers.put(type, new Mapifier(object -> apply0(getMapifier(type).mapify, object) //
					, object -> apply0(getMapifier(type).unmapify, object)));
			mapifiers.put(type, mapifier = newMapifier(type));
		}
		return mapifier;
	}

	@SuppressWarnings("unchecked")
	private Mapifier newMapifier(Type type) {
		Mapifier mapifier;

		if (type instanceof Class) {
			Class<?> clazz = (Class<?>) type;

			if (isDirectlyMapped(clazz))
				mapifier = new Mapifier(id, id);
			else if (clazz.isArray()) {
				Class<?> componentType = clazz.getComponentType();
				Mapifier mapifier1 = getMapifier(componentType);
				mapifier = new Mapifier(object -> {
					Map<Object, Object> map = newMap();
					int length = Array.getLength(object);
					for (int i = 0; i < length; i++)
						map.put(i, apply0(mapifier1.mapify, Array.get(object, i)));
					return map;
				}, object -> {
					Map<?, ?> map = (Map<?, ?>) object;
					Object objects = Array.newInstance(componentType, map.size());
					int i = 0;
					while (map.containsKey(i)) {
						Array.set(objects, i, apply0(mapifier1.unmapify, map.get(i)));
						i++;
					}
					return objects;
				});
			} else if (clazz.isInterface() || Modifier.isAbstract(clazz.getModifiers())) // polymorphism
				mapifier = new Mapifier(object -> {
					Class<?> clazz1 = object.getClass();
					Object m = apply0(getMapifier(clazz1).mapify, object);
					if (m instanceof Map) {
						Map<String, String> map = (Map<String, String>) m;
						map.put("@class", clazz1.getName());
						return map;
					} else
						// happens when an enum implements an interface
						return m;
				}, object -> {
					if (object instanceof Map) {
						Map<?, ?> map = (Map<?, ?>) object;
						String className = map.get("@class").toString();
						Class<?> clazz1 = Rethrow.ex(() -> Class.forName(className));
						return apply0(getMapifier(clazz1).unmapify, object);
					} else
						// happens when an enum implements an interface
						return object;
				});
			else {
				List<FieldInfo> fis = Read.from(inspect.fields(clazz)) //
						.map(field -> {
							Type type1 = field.getGenericType();
							return new FieldInfo(field, field.getName(), getMapifier(type1));
						}) //
						.toList();

				mapifier = new Mapifier(object -> Rethrow.ex(() -> {
					Map<Object, Object> map = newMap();
					for (FieldInfo fi : fis)
						map.put(fi.name, apply0(fi.mapifier.mapify, fi.field.get(object)));
					return map;
				}), object -> Rethrow.ex(() -> {
					Map<?, ?> map = (Map<?, ?>) object;
					Object object1 = clazz.newInstance();
					for (FieldInfo fi : fis)
						fi.field.set(object1, apply0(fi.mapifier.unmapify, map.get(fi.name)));
					return object1;
				}));
			}
		} else if (type instanceof ParameterizedType) {
			ParameterizedType pt = (ParameterizedType) type;
			Type rawType = pt.getRawType();
			Type[] typeArguments = pt.getActualTypeArguments();
			Class<?> clazz = rawType instanceof Class ? (Class<?>) rawType : null;

			if (collectionClasses.contains(clazz)) {
				Mapifier mapifier1 = getMapifier(typeArguments[0]);
				mapifier = new Mapifier(object -> {
					Map<Object, Object> map = newMap();
					int i = 0;
					for (Object o : (Collection<?>) object)
						map.put(i++, apply0(mapifier1.mapify, o));
					return map;
				}, object -> {
					Map<?, ?> map = (Map<?, ?>) object;
					Collection<Object> object1 = (Collection<Object>) instantiate(clazz);
					int i = 0;
					while (map.containsKey(i))
						object1.add(apply0(mapifier1.unmapify, map.get(i++)));
					return object1;
				});
			} else if (mapClasses.contains(clazz)) {
				Mapifier km = getMapifier(typeArguments[0]);
				Mapifier vm = getMapifier(typeArguments[1]);
				mapifier = new Mapifier(object -> {
					Map<Object, Object> map = newMap();
					for (Entry<?, ?> e : ((Map<?, ?>) object).entrySet())
						map.put(apply0(km.mapify, e.getKey()), apply0(vm.mapify, e.getValue()));
					return map;
				}, object -> {
					Map<?, ?> map = (Map<?, ?>) object;
					Map<Object, Object> object1 = (Map<Object, Object>) instantiate(clazz);
					for (Entry<?, ?> e : map.entrySet())
						object1.put(apply0(km.unmapify, e.getKey()), apply0(vm.unmapify, e.getValue()));
					return object1;
				});
			} else
				mapifier = getMapifier(rawType);
		} else
			throw new RuntimeException("Unrecognized type " + type);

		return mapifier;
	}

	private boolean isDirectlyMapped(Class<?> clazz) {
		return clazz.isPrimitive() //
				|| clazz.isEnum() //
				|| clazz == Boolean.class //
				|| clazz == String.class //
				|| Number.class.isAssignableFrom(clazz);
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
			return Rethrow.ex(clazz::newInstance);

		@SuppressWarnings("unchecked")
		T t = (T) object;
		return t;
	}

	private Object apply0(Fun<Object, Object> fun, Object object) {
		return object != null ? fun.apply(object) : null;
	}

	private Map<Object, Object> newMap() {
		return new HashMap<>();
	}

}
