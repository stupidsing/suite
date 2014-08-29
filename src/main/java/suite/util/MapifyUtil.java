package suite.util;

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

import suite.util.FunUtil.Fun;

/**
 * Convert (supposedly) any Java structures to recursive maps.
 *
 * @author ywsing
 */
public class MapifyUtil {

	private Set<Class<?>> collectionClasses = new HashSet<>(Arrays.asList( //
			ArrayList.class, Collection.class, HashSet.class, List.class, Set.class));
	private Set<Class<?>> mapClasses = new HashSet<>(Arrays.asList( //
			HashMap.class, Map.class));

	private Fun<Object, Object> id = object -> object;

	private Map<Class<?>, Fun<Object, Object>> mapifiers = new ConcurrentHashMap<>();
	private Map<Class<?>, Fun<Object, Object>> unmapifiers = new ConcurrentHashMap<>();

	private InspectUtil inspectUtil;

	private class FieldInfo {
		private Field field;
		private String name;
		private Fun<Object, Object> mapifier;
		private Fun<Object, Object> unmapifier;

		private FieldInfo(Field field, String name, Fun<Object, Object> mapifier, Fun<Object, Object> unmapifier) {
			this.field = field;
			this.name = name;
			this.mapifier = mapifier;
			this.unmapifier = unmapifier;
		}
	}

	public MapifyUtil(InspectUtil inspectUtil) {
		this.inspectUtil = inspectUtil;
	}

	public <T> Object mapify(Class<T> clazz, T t) {
		if (t != null)
			return getMapifier(clazz).apply(t);
		else
			return null;
	}

	public <T> T unmapify(Class<T> clazz, Object object) {
		if (object != null) {
			@SuppressWarnings("unchecked")
			T t = (T) getUnmapifier(clazz).apply(object);
			return t;
		} else
			return null;
	}

	private Fun<Object, Object> getMapifier(Class<?> clazz) {
		Fun<Object, Object> mapifier = mapifiers.get(clazz);
		if (mapifier == null) {
			mapifiers.put(clazz, object -> getMapifier(clazz));
			mapifiers.put(clazz, mapifier = createMapifier0(clazz));
		}
		return mapifier;
	}

	private Fun<Object, Object> getUnmapifier(Class<?> clazz) {
		Fun<Object, Object> unmapifier = unmapifiers.get(clazz);
		if (unmapifier == null) {
			unmapifiers.put(clazz, object -> getUnmapifier(clazz));
			unmapifiers.put(clazz, unmapifier = createUnmapifier0(clazz));
		}
		return unmapifier;
	}

	private Fun<Object, Object> createMapifier0(Type type) {
		if (type instanceof Class) {
			Class<?> clazz = (Class<?>) type;

			if (isDirectlyMapped(clazz))
				return id;
			else if (clazz.isArray()) {
				Fun<Object, Object> mapifier1 = createMapifier0(clazz.getComponentType());
				return object -> {
					Map<Object, Object> map = newMap();
					Object objects[] = (Object[]) object;
					for (int i = 0; i < objects.length; i++)
						map.put(i, apply0(mapifier1, objects[i]));
					return map;
				};
			} else if (clazz.isInterface()) // Polymorphism
				return object -> {
					Class<? extends Object> clazz1 = object.getClass();
					@SuppressWarnings("unchecked")
					Map<String, String> map = (Map<String, String>) getMapifier(clazz1).apply(object);
					map.put("@class", clazz1.getName());
					return map;
				};
			else {
				List<FieldInfo> fieldInfos = getFieldInfos(clazz);
				return object -> {
					Map<Object, Object> map = newMap();
					for (FieldInfo fieldInfo : fieldInfos)
						try {
							map.put(fieldInfo.name, apply0(fieldInfo.mapifier, fieldInfo.field.get(object)));
						} catch (ReflectiveOperationException ex) {
							throw new RuntimeException(ex);
						}
					return map;
				};
			}
		} else if (type instanceof ParameterizedType) {
			ParameterizedType pt = (ParameterizedType) type;
			Type rawType = pt.getRawType();
			Type typeArguments[] = pt.getActualTypeArguments();
			Class<?> clazz = rawType instanceof Class ? (Class<?>) rawType : null;

			if (collectionClasses.contains(clazz)) {
				Fun<Object, Object> mapifier1 = createMapifier0(typeArguments[0]);
				return object -> {
					Map<Object, Object> map = newMap();
					int i = 0;
					for (Object o : (Collection<?>) object)
						map.put(i++, apply0(mapifier1, o));
					return map;
				};
			} else if (mapClasses.contains(clazz)) {
				Fun<Object, Object> keyMapifier = createMapifier0(typeArguments[0]);
				Fun<Object, Object> valueMapifier = createMapifier0(typeArguments[1]);
				return object -> {
					Map<Object, Object> map = newMap();
					for (Entry<?, ?> e : ((Map<?, ?>) object).entrySet())
						map.put(apply0(keyMapifier, e.getKey()), apply0(valueMapifier, e.getValue()));
					return map;
				};
			} else
				return createMapifier0(rawType);
		}

		throw new RuntimeException("Unrecognized type " + type);
	}

	private Fun<Object, Object> createUnmapifier0(Type type) {
		if (type instanceof Class) {
			Class<?> clazz = (Class<?>) type;

			if (isDirectlyMapped(clazz))
				return id;
			else if (clazz.isArray()) {
				Fun<Object, Object> unmapifier1 = createUnmapifier0(clazz.getComponentType());
				return object -> {
					Map<?, ?> map = (Map<?, ?>) object;
					Object objects[] = new Object[map.size()];
					int i = 0;
					while (map.containsKey(i))
						objects[i] = apply0(unmapifier1, map.get(i++));
					return objects;
				};
			} else if (clazz.isInterface()) // Polymorphism
				return object -> {
					Map<?, ?> map = (Map<?, ?>) object;
					Class<?> clazz1;
					try {
						clazz1 = Class.forName(map.get("@class").toString());
					} catch (ClassNotFoundException ex) {
						throw new RuntimeException(ex);
					}
					return getUnmapifier(clazz1).apply(object);
				};
			else {
				List<FieldInfo> fieldInfos = getFieldInfos(clazz);
				return object -> {
					Map<?, ?> map = (Map<?, ?>) object;
					try {
						Object object1 = clazz.newInstance();
						for (FieldInfo fieldInfo : fieldInfos)
							fieldInfo.field.set(object1, apply0(fieldInfo.unmapifier, map.get(fieldInfo.name)));
						return object1;
					} catch (ReflectiveOperationException ex) {
						throw new RuntimeException(ex);
					}
				};
			}
		} else if (type instanceof ParameterizedType) {
			ParameterizedType pt = (ParameterizedType) type;
			Type rawType = pt.getRawType();
			Type typeArguments[] = pt.getActualTypeArguments();
			Class<?> clazz = rawType instanceof Class ? (Class<?>) rawType : null;

			if (collectionClasses.contains(clazz)) {
				Fun<Object, Object> unmapifier1 = createUnmapifier0(typeArguments[0]);
				return object -> {
					Map<?, ?> map = (Map<?, ?>) object;
					@SuppressWarnings("unchecked")
					Collection<Object> object1 = (Collection<Object>) create(clazz);
					int i = 0;
					while (map.containsKey(i))
						object1.add(apply0(unmapifier1, map.get(i++)));
					return object1;
				};
			} else if (mapClasses.contains(clazz)) {
				Fun<Object, Object> keyUnmapifier = createUnmapifier0(typeArguments[0]);
				Fun<Object, Object> valueUnmapifier = createUnmapifier0(typeArguments[1]);
				return object -> {
					Map<?, ?> map = (Map<?, ?>) object;
					@SuppressWarnings("unchecked")
					Map<Object, Object> object1 = (Map<Object, Object>) create(clazz);
					for (Entry<?, ?> e : map.entrySet())
						object1.put(apply0(keyUnmapifier, e.getKey()), apply0(valueUnmapifier, e.getValue()));
					return object1;
				};
			} else
				return createMapifier0(rawType);
		}

		throw new RuntimeException("Unrecognized type " + type);
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
		List<Field> fields = inspectUtil.getFields(clazz);
		List<FieldInfo> fieldInfos = new ArrayList<>();

		for (Field field : fields) {
			Type type = field.getGenericType();
			fieldInfos.add(new FieldInfo(field, field.getName(), createMapifier0(type), createUnmapifier0(type)));
		}
		return fieldInfos;
	}

	private Object apply0(Fun<Object, Object> fun, Object object) {
		return object != null ? fun.apply(object) : null;
	}

	private Map<Object, Object> newMap() {
		return new HashMap<>();
	}

}
