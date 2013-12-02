package suite.util;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

/**
 * .. Converts (supposedly) any Java structures to a recursive tagged list of
 * key-value format.
 * 
 * @author ywsing
 */
public class InspectUtil {

	private static InspectUtil instance = new InspectUtil();

	private Map<Class<?>, ClassInformation> clazzInformation = new ConcurrentHashMap<>();

	private class ClassInformation {
		private List<Field> fields;
	}

	public static Map<Object, Object> toMap(Object object) {
		Map<Object, Object> result = new HashMap<>();
		String className;

		if (object == null)
			className = "null";
		else {
			Class<?> clazz = object.getClass();
			className = clazz.getCanonicalName();

			if (clazz == Array.class) {
				Object[] a = (Object[]) object;
				for (int i = 0; i < a.length; i++)
					result.put(i, toMap(a[i]));
			} else if (Collection.class.isAssignableFrom(clazz)) {
				Collection<?> col = (Collection<?>) object;
				Iterator<?> iter = col.iterator();
				int i = 0;

				while (iter.hasNext())
					result.put(i++, toMap(col.iterator().next()));
			} else if (Map.class.isAssignableFrom(clazz))
				for (Entry<?, ?> entry : ((Map<?, ?>) object).entrySet())
					result.put(toMap(entry.getKey()), toMap(entry.getValue()));
			else if (clazz.isPrimitive() || clazz == String.class)
				result.put("value", object);
			else
				for (Field field : getFields(object))
					try {
						result.put(toMap(field.getName()), toMap(field.get(object)));
					} catch (IllegalAccessException ex) {
						throw new RuntimeException(ex);
					}
		}

		result.put("@class", className);
		return result;
	}

	public static <T> boolean equals(T o0, T o1) {
		boolean result;
		if (o0 != o1)
			result = o0 != null && o1 != null //
					&& o0.getClass() == o1.getClass() //
					&& Util.equals(toList(o0), toList(o1));
		else
			result = true;
		return result;
	}

	public static int hashCode(Object object) {
		return toList(object).hashCode();
	}

	private static List<Object> toList(Object object) {
		List<Object> list = new ArrayList<>();

		for (Field field : getFields(object))
			try {
				list.add(field.get(object));
			} catch (IllegalAccessException ex) {
				throw new RuntimeException(ex);
			}

		return list;
	}

	private static List<Field> getFields(Object object) {
		return instance.getClassInformation(object.getClass()).fields;
	}

	private ClassInformation getClassInformation(Class<?> clazz) {
		ClassInformation ci = clazzInformation.get(clazz);
		if (ci == null)
			clazzInformation.put(clazz, ci = getClassInformation0(clazz));
		return ci;
	}

	private ClassInformation getClassInformation0(Class<?> clazz) {
		ClassInformation parent = getClassInformation(clazz.getSuperclass());

		ClassInformation ci = new ClassInformation();
		ci.fields = new ArrayList<>(parent.fields);

		for (Field field : clazz.getDeclaredFields()) {
			field.setAccessible(true);
			ci.fields.add(field);
		}

		return ci;
	}

}
