package suite.util;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class InspectUtil {

	private static InspectUtil instance = new InspectUtil();

	private Map<Class<?>, ClassInformation> clazzInformation = new ConcurrentHashMap<>();

	private class ClassInformation {
		private List<Field> fields;
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

	public static List<Object> toList(Object object) {
		List<Object> list = new ArrayList<>();
		for (Field field : getFields(object))
			try {
				list.add(field.get(object));
			} catch (IllegalAccessException ex) {
				throw new RuntimeException(ex);
			}
		return list;
	}

	public static Map<String, Object> toMap(Object object) {
		Map<String, Object> map = new HashMap<>();
		for (Field field : getFields(object))
			try {
				map.put(field.getName(), field.get(object));
			} catch (IllegalAccessException ex) {
				throw new RuntimeException(ex);
			}
		return map;
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
