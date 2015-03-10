package suite.inspect;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
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

/**
 * Convert (supposedly) any Java structures to recursive maps.
 *
 * @author ywsing
 */
public class Mapify {

	private Set<Class<?>> collectionClasses = new HashSet<>(Arrays.asList( //
			ArrayList.class, Collection.class, HashSet.class, List.class, Set.class));
	private Set<Class<?>> mapClasses = new HashSet<>(Arrays.asList( //
			HashMap.class, Map.class));

	private Fun<Object, Object> id = object -> object;

	private Map<Class<?>, Mapifier> mapifiers = new ConcurrentHashMap<>();

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
		if (t != null)
			return getMapifier(clazz).mapify.apply(t);
		else
			return null;
	}

	public <T> T unmapify(Class<T> clazz, Object object) {
		if (object != null) {
			@SuppressWarnings("unchecked")
			T t = (T) getMapifier(clazz).unmapify.apply(object);
			return t;
		} else
			return null;
	}

	private Mapifier getMapifier(Class<?> clazz) {
		Mapifier mapifier = mapifiers.get(clazz);
		if (mapifier == null) {
			mapifiers.put(clazz, new Mapifier(object -> getMapifier(clazz).mapify.apply(object),
					object -> getMapifier(clazz).unmapify.apply(object)));
			mapifiers.put(clazz, mapifier = createMapifier(clazz));
		}
		return mapifier;
	}

	@SuppressWarnings("unchecked")
	private Mapifier createMapifier(Type type) {
		Mapifier mapifier;

		if (type instanceof Class) {
			Class<?> clazz = (Class<?>) type;

			if (isDirectlyMapped(clazz))
				mapifier = new Mapifier(id, id);
			else if (clazz.isArray()) {
				Class<?> componentType = clazz.getComponentType();
				Mapifier mapifier1 = createMapifier(componentType);
				if (componentType.isPrimitive())
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
				else
					mapifier = new Mapifier(object -> {
						Map<Object, Object> map = newMap();
						Object objects[] = (Object[]) object;
						for (int i = 0; i < objects.length; i++)
							map.put(i, apply0(mapifier1.mapify, objects[i]));
						return map;
					}, object -> {
						Map<?, ?> map = (Map<?, ?>) object;
						Object objects[] = new Object[map.size()];
						int i = 0;
						while (map.containsKey(i))
							objects[i] = apply0(mapifier1.unmapify, map.get(i++));
						return objects;
					});
			} else if (clazz.isInterface()) // Polymorphism
				mapifier = new Mapifier(object -> {
					Class<?> clazz1 = object.getClass();
					Object m = getMapifier(clazz1).mapify.apply(object);
					if (m instanceof Map) {
						Map<String, String> map = (Map<String, String>) m;
						map.put("@class", clazz1.getName());
						return map;
					} else
						// Happens when an enum implements an interface
						return m;
				}, object -> {
					if (object instanceof Map) {
						Map<?, ?> map = (Map<?, ?>) object;
						Class<?> clazz1;
						try {
							clazz1 = Class.forName(map.get("@class").toString());
						} catch (ClassNotFoundException ex) {
							throw new RuntimeException(ex);
						}
						return getMapifier(clazz1).unmapify.apply(object);
					} else
						// Happens when an enum implements an interface
						return object;
				});
			else {
				List<FieldInfo> fis = getFieldInfos(clazz);
				mapifier = new Mapifier(object -> {
					Map<Object, Object> map = newMap();
					for (FieldInfo fi : fis)
						try {
							map.put(fi.name, apply0(fi.mapifier.mapify, fi.field.get(object)));
						} catch (ReflectiveOperationException ex) {
							throw new RuntimeException(ex);
						}
					return map;
				}, object -> {
					Map<?, ?> map = (Map<?, ?>) object;
					try {
						Object object1 = clazz.newInstance();
						for (FieldInfo fi : fis)
							fi.field.set(object1, apply0(fi.mapifier.unmapify, map.get(fi.name)));
						return object1;
					} catch (ReflectiveOperationException ex) {
						throw new RuntimeException(ex);
					}
				});
			}
		} else if (type instanceof ParameterizedType) {
			ParameterizedType pt = (ParameterizedType) type;
			Type rawType = pt.getRawType();
			Type typeArguments[] = pt.getActualTypeArguments();
			Class<?> clazz = rawType instanceof Class ? (Class<?>) rawType : null;

			if (collectionClasses.contains(clazz)) {
				Mapifier mapifier1 = createMapifier(typeArguments[0]);
				mapifier = new Mapifier(object -> {
					Map<Object, Object> map = newMap();
					int i = 0;
					for (Object o : (Collection<?>) object)
						map.put(i++, apply0(mapifier1.mapify, o));
					return map;
				}, object -> {
					Map<?, ?> map = (Map<?, ?>) object;
					Collection<Object> object1 = (Collection<Object>) create(clazz);
					int i = 0;
					while (map.containsKey(i))
						object1.add(apply0(mapifier1.unmapify, map.get(i++)));
					return object1;
				});
			} else if (mapClasses.contains(clazz)) {
				Mapifier km = createMapifier(typeArguments[0]);
				Mapifier vm = createMapifier(typeArguments[1]);
				mapifier = new Mapifier(object -> {
					Map<Object, Object> map = newMap();
					for (Entry<?, ?> e : ((Map<?, ?>) object).entrySet())
						map.put(apply0(km.mapify, e.getKey()), apply0(vm.mapify, e.getValue()));
					return map;
				}, object -> {
					Map<?, ?> map = (Map<?, ?>) object;
					Map<Object, Object> object1 = (Map<Object, Object>) create(clazz);
					for (Entry<?, ?> e : map.entrySet())
						object1.put(apply0(km.unmapify, e.getKey()), apply0(vm.unmapify, e.getValue()));
					return object1;
				});
			} else
				mapifier = createMapifier(rawType);
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

	private <T> T create(Class<T> clazz) {
		try {
			Object object;
			if (clazz == ArrayList.class || clazz == Collection.class || clazz == List.class)
				object = new ArrayList<>();
			else if (clazz == HashSet.class || clazz == Set.class)
				object = new HashSet<>();
			else if (clazz == HashMap.class || clazz == Map.class)
				object = new HashMap<>();
			else
				return clazz.newInstance();

			@SuppressWarnings("unchecked")
			T t = (T) object;
			return t;
		} catch (ReflectiveOperationException ex) {
			throw new RuntimeException(ex);
		}
	}

	private List<FieldInfo> getFieldInfos(Class<?> clazz) {
		return Read.from(inspect.fields(clazz)) //
				.map(field -> {
					Type type = field.getGenericType();
					return new FieldInfo(field, field.getName(), createMapifier(type));
				}) //
				.toList();
	}

	private Object apply0(Fun<Object, Object> fun, Object object) {
		return object != null ? fun.apply(object) : null;
	}

	private Map<Object, Object> newMap() {
		return new HashMap<>();
	}

}
