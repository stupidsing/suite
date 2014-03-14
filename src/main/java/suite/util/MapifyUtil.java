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
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;

import suite.util.FunUtil.Fun;

/**
 * Converts (supposedly) any Java structures to a recursive map.
 * 
 * @author ywsing
 */
public class MapifyUtil {

	private Set<Class<?>> collectionClasses = new HashSet<>(Arrays.<Class<?>> asList( //
			ArrayList.class, Collection.class, HashSet.class, List.class, Set.class));
	private Set<Class<?>> mapClasses = new HashSet<>(Arrays.<Class<?>> asList( //
			HashMap.class, Map.class, TreeMap.class));

	private Fun<Object, Object> id = new Fun<Object, Object>() {
		public Object apply(Object object) {
			return object;
		}
	};

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

	public Object mapify(Object object) {
		if (object != null)
			return getMapifier(object.getClass()).apply(object);
		else
			return null;
	}

	public Object unmapify(Object object) {
		if (object != null)
			return getUnmapifier(object.getClass()).apply(object);
		else
			return null;
	}

	private Fun<Object, Object> getMapifier(final Class<?> clazz) {
		Fun<Object, Object> mapifier = mapifiers.get(clazz);
		if (mapifier == null) {
			mapifiers.put(clazz, new Fun<Object, Object>() {
				public Object apply(Object object) {
					return getMapifier(clazz);
				}
			});
			mapifiers.put(clazz, mapifier = createMapifier0(clazz));
		}
		return mapifier;
	}

	private Fun<Object, Object> getUnmapifier(final Class<?> clazz) {
		Fun<Object, Object> unmapifier = unmapifiers.get(clazz);
		if (unmapifier == null) {
			unmapifiers.put(clazz, new Fun<Object, Object>() {
				public Object apply(Object object) {
					return getUnmapifier(clazz);
				}
			});
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
				final Fun<Object, Object> mapifier1 = createMapifier0(clazz.getComponentType());
				return new Fun<Object, Object>() {
					public Object apply(Object object) {
						Map<Object, Object> map = newMap();
						Object objects[] = (Object[]) object;
						for (int i = 0; i < objects.length; i++)
							map.put(i, apply0(mapifier1, objects[i]));
						return map;
					}
				};
			} else {
				final List<FieldInfo> fieldInfos = getFieldInfos(clazz);
				return new Fun<Object, Object>() {
					public Object apply(Object object) {
						Map<Object, Object> map = newMap();
						for (FieldInfo fieldInfo : fieldInfos)
							try {
								map.put(fieldInfo.name, apply0(fieldInfo.mapifier, fieldInfo.field.get(object)));
							} catch (ReflectiveOperationException ex) {
								throw new RuntimeException(ex);
							}
						return map;
					}
				};
			}
		} else if (type instanceof ParameterizedType) {
			ParameterizedType pt = (ParameterizedType) type;
			Type rawType = pt.getRawType();
			Type typeArguments[] = pt.getActualTypeArguments();
			Class<?> clazz = rawType instanceof Class ? (Class<?>) rawType : null;

			if (collectionClasses.contains(clazz)) {
				final Fun<Object, Object> mapifier1 = createMapifier0(typeArguments[0]);
				return new Fun<Object, Object>() {
					public Object apply(Object object) {
						Map<Object, Object> map = newMap();
						int i = 0;
						for (Object o : (Collection<?>) object)
							map.put(i++, apply0(mapifier1, o));
						return map;
					}
				};
			} else if (mapClasses.contains(clazz)) {
				final Fun<Object, Object> keyMapifier = createMapifier0(typeArguments[0]);
				final Fun<Object, Object> valueMapifier = createMapifier0(typeArguments[1]);
				return new Fun<Object, Object>() {
					public Object apply(Object object) {
						Map<Object, Object> map = newMap();
						for (Entry<?, ?> e : ((Map<?, ?>) object).entrySet())
							map.put(apply0(keyMapifier, e.getKey()), apply0(valueMapifier, e.getValue()));
						return map;
					}
				};
			} else
				return createMapifier0(rawType);
		}

		throw new RuntimeException("Unrecognized type " + type);
	}

	private Fun<Object, Object> createUnmapifier0(Type type) {
		if (type instanceof Class) {
			final Class<?> clazz = (Class<?>) type;

			if (isDirectlyMapped(clazz))
				return id;
			else if (clazz.isArray()) {
				final Fun<Object, Object> unmapifier1 = createUnmapifier0(clazz.getComponentType());
				return new Fun<Object, Object>() {
					public Object apply(Object object) {
						Map<?, ?> map = (Map<?, ?>) object;
						Object objects[] = new Object[map.size()];
						int i = 0;
						while (map.containsKey(i))
							objects[i] = apply0(unmapifier1, map.get(i++));
						return objects;
					}
				};
			} else {
				final List<FieldInfo> fieldInfos = getFieldInfos(clazz);
				return new Fun<Object, Object>() {
					public Object apply(Object object) {
						Map<?, ?> map = (Map<?, ?>) object;
						try {
							Object object1 = clazz.newInstance();
							for (FieldInfo fieldInfo : fieldInfos)
								fieldInfo.field.set(object1, apply0(fieldInfo.unmapifier, map.get(fieldInfo.name)));
							return object1;
						} catch (ReflectiveOperationException ex) {
							throw new RuntimeException(ex);
						}
					}
				};
			}
		} else if (type instanceof ParameterizedType) {
			ParameterizedType pt = (ParameterizedType) type;
			Type rawType = pt.getRawType();
			Type typeArguments[] = pt.getActualTypeArguments();
			final Class<?> clazz = rawType instanceof Class ? (Class<?>) rawType : null;

			if (collectionClasses.contains(clazz)) {
				final Fun<Object, Object> unmapifier1 = createUnmapifier0(typeArguments[0]);
				return new Fun<Object, Object>() {
					public Object apply(Object object) {
						Map<?, ?> map = (Map<?, ?>) object;
						@SuppressWarnings("unchecked")
						Collection<Object> object1 = (Collection<Object>) create(clazz);
						int i = 0;
						while (map.containsKey(i))
							object1.add(apply0(unmapifier1, map.get(i++)));
						return object1;
					}
				};
			} else if (mapClasses.contains(clazz)) {
				final Fun<Object, Object> keyUnmapifier = createUnmapifier0(typeArguments[0]);
				final Fun<Object, Object> valueUnmapifier = createUnmapifier0(typeArguments[1]);
				return new Fun<Object, Object>() {
					public Object apply(Object object) {
						Map<?, ?> map = (Map<?, ?>) object;
						@SuppressWarnings("unchecked")
						Map<Object, Object> object1 = (Map<Object, Object>) create(clazz);
						for (Entry<?, ?> e : map.entrySet())
							object1.put(apply0(keyUnmapifier, e.getKey()), apply0(valueUnmapifier, e.getValue()));
						return object1;
					}
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

	private List<FieldInfo> getFieldInfos(Class<?> clazz) {
		final List<Field> fields = inspectUtil.getFields(clazz);
		final List<FieldInfo> fieldInfos = new ArrayList<>();

		for (Field field : fields) {
			Type type = field.getGenericType();
			fieldInfos.add(new FieldInfo(field, field.getName(), createMapifier0(type), createUnmapifier0(type)));
		}
		return fieldInfos;
	}

	private <T> T create(Class<T> clazz) {
		try {
			Object object;
			if (clazz == Collection.class)
				object = new ArrayList<>();
			else if (clazz == List.class)
				object = new ArrayList<>();
			else if (clazz == Map.class)
				object = new HashMap<>();
			else if (clazz == Set.class)
				object = new HashSet<>();
			else
				return clazz.newInstance();

			@SuppressWarnings("unchecked")
			T t = (T) object;
			return t;
		} catch (ReflectiveOperationException ex) {
			throw new RuntimeException(ex);
		}
	}

	private Object apply0(Fun<Object, Object> fun, Object object) {
		return object != null ? fun.apply(object) : null;
	}

	private Map<Object, Object> newMap() {
		return new HashMap<>();
	}

}
