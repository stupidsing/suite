package org.util;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.sql.Timestamp;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

public class DumpUtil {

	private Set<Integer> dumpedIds = new HashSet<>();

	private DumpUtil() {
	}

	public static String dump(Object object) {
		return dump("", object);
	}

	public static String dump(String prefix, Object object) {
		StringBuilder sb = new StringBuilder();
		dump(prefix, object, sb);
		return sb.toString();
	}

	/**
	 * Dumps object content (public data and getters) through Reflection to a
	 * string buffer, line-by-line. Probably you want to use the easier 1 or
	 * 2-parameter(s) version to output to log4j.
	 * 
	 * Beware that if you pass in a recursive structure, this would gone crazily
	 * stack overflow. And this could slow your program down by the heavy use of
	 * reflections.
	 * 
	 * No private fields dump yet, since it requires some setting in Java
	 * security manager.
	 * 
	 * @param prefix
	 *            To be appended before each line.
	 * @param object
	 *            The monster.
	 * @param sb
	 *            String buffer to hold the dumped content.
	 */
	public static void dump(String prefix, Object object, StringBuilder sb) {
		if (object != null)
			new DumpUtil().dump(prefix, object, object.getClass(), sb);
		else
			new DumpUtil().dump(prefix, object, void.class, sb);
	}

	public void dump(String prefix, Object object, Class<?> clazz,
			StringBuilder sb) {
		sb.append(prefix);
		sb.append(" =");
		if (object == null) {
			sb.append(" null\n");
			return;
		}

		int id = System.identityHashCode(object);

		if (!dumpedIds.add(id)) {
			sb.append(" <<recursed>>");
			return;
		} else if (clazz == String.class)
			sb.append(" \"" + object + "\"");
		else if (!Collection.class.isAssignableFrom(clazz))
			sb.append(" " + object);
		sb.append(" [" + clazz.getSimpleName() + "]\n");

		// Some easy classes do not require windy listings
		if (isSimpleType(clazz))
			return;

		for (Field field : clazz.getFields())
			if (!Modifier.isStatic(field.getModifiers()))
				try {
					String name = field.getName();
					Object o = field.get(object);
					Class<?> type = field.getType();
					if (isSimpleType(type))
						dump(prefix + "." + name, o, type, sb);
					else
						dump(prefix + "." + name, o, sb);
				} catch (Throwable ex) {
					sb.append(prefix + "." + field.getName());
					sb.append(" caught " + ex + "\n");
				}

		Set<String> displayedMethod = new HashSet<>();
		for (Method method : clazz.getMethods()) {
			String name = method.getName();
			try {
				if (name.startsWith("get")
						&& method.getParameterTypes().length == 0
						&& !displayedMethod.contains(name)) {
					Object o = method.invoke(object);
					if (!(o instanceof Class<?>))
						dump(prefix + "." + name + "()", o, sb);

					// Do not display same method of different base classes
					displayedMethod.add(name);
				}
			} catch (Throwable ex) {
				sb.append(prefix + "." + name + "()");
				sb.append(" caught " + ex + "\n");
			}
		}

		int count = 0;

		if (clazz.isArray())
			if (clazz.getComponentType() == int.class)
				for (int i : (int[]) object)
					dump(prefix + "[" + count++ + "]", i, sb);
			else if (Object.class.isAssignableFrom(clazz.getComponentType()))
				for (Object o1 : (Object[]) object)
					dump(prefix + "[" + count++ + "]", o1, sb);

		if (Collection.class.isAssignableFrom(clazz))
			for (Object o1 : (Collection<?>) object)
				dump(prefix + "[" + count++ + "]", o1, sb);
		else if (Map.class.isAssignableFrom(clazz)) {
			for (Entry<?, ?> entry : ((Map<?, ?>) object).entrySet()) {
				Object key = entry.getKey(), value = entry.getValue();
				dump(prefix + "[" + count + "].getKey()", key, sb);
				dump(prefix + "[" + count + "].getValue()", value, sb);
				count++;
			}
		}
	}

	/**
	 * Types that do not require per-member dump.
	 */
	private static boolean isSimpleType(Class<?> clazz) {
		return clazz.isPrimitive() || clazz == String.class
				|| clazz == Date.class || clazz == Timestamp.class;
	}

}
