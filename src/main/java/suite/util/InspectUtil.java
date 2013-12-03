package suite.util;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;

/**
 * .. Converts (supposedly) any Java structures to a recursive map.
 * 
 * @author ywsing
 */
public class InspectUtil {

	private static InspectUtil instance = new InspectUtil();

	private Map<Class<?>, List<Field>> fieldsByClass = new ConcurrentHashMap<>();

	private InspectUtil() {
	}

	public static Object mapify(Object object) {
		Object result;

		if (object != null) {
			Class<?> clazz = object.getClass();

			if (clazz.isPrimitive() //
					|| clazz.isEnum() //
					|| clazz == Boolean.class //
					|| clazz == String.class //
					|| Number.class.isAssignableFrom(clazz))
				result = object;
			else {
				Map<Object, Object> map;

				if (clazz.isArray())
					map = mapify(Arrays.asList((Object[]) object));
				else if (Set.class.isAssignableFrom(clazz))
					map = mapify(new TreeSet<>((Set<?>) object));
				else if (Collection.class.isAssignableFrom(clazz))
					map = mapify((Collection<?>) object);
				else if (Map.class.isAssignableFrom(clazz)) {
					map = new HashMap<>();
					for (Entry<?, ?> entry : ((Map<?, ?>) object).entrySet())
						map.put(mapify(entry.getKey()), mapify(entry.getValue()));
				} else {
					map = new HashMap<>();
					for (Field field : instance.getFields(object))
						try {
							map.put(mapify(field.getName()), mapify(field.get(object)));
						} catch (IllegalAccessException ex) {
							throw new RuntimeException(ex);
						}
				}

				map.put("@class", clazz.getCanonicalName());
				result = map;
			}
		} else
			result = null;

		return result;
	}

	private static Map<Object, Object> mapify(Collection<?> col) {
		int i = 0;
		Map<Object, Object> map = new HashMap<>();
		for (Object elem : col)
			map.put(i++, mapify(elem));
		return map;
	}

	public static <T> boolean equals(T o0, T o1) {
		boolean result;
		if (o0 != o1)
			result = o0 != null && o1 != null //
					&& o0.getClass() == o1.getClass() //
					&& Util.equals(instance.toList(o0), instance.toList(o1));
		else
			result = true;
		return result;
	}

	public static int hashCode(Object object) {
		return instance.toList(object).hashCode();
	}

	private List<Object> toList(Object object) {
		List<Object> list = new ArrayList<>();

		for (Field field : getFields(object))
			try {
				list.add(field.get(object));
			} catch (IllegalAccessException ex) {
				throw new RuntimeException(ex);
			}

		return list;
	}

	private List<Field> getFields(Object object) {
		return getFields(object.getClass());
	}

	private List<Field> getFields(Class<?> clazz) {
		List<Field> ci = fieldsByClass.get(clazz);
		if (ci == null)
			fieldsByClass.put(clazz, ci = getFields0(clazz));
		return ci;
	}

	private List<Field> getFields0(Class<?> clazz) {
		List<Field> parentFields = getFields(clazz.getSuperclass());
		List<Field> fields = new ArrayList<>(parentFields);

		for (Field field : clazz.getDeclaredFields())
			if (!Modifier.isStatic(field.getModifiers())) {
				field.setAccessible(true);
				fields.add(field);
			}

		return fields;
	}

}
