package suite.inspect;

import java.lang.reflect.Array;
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

public class Dump {

	private Set<Integer> dumpedIds = new HashSet<>();
	private StringBuilder sb = new StringBuilder();

	public Dump() {
		this(new StringBuilder());
	}

	public Dump(StringBuilder sb) {
		this.sb = sb;
	}

	public static String object(Object object) {
		return object("", object);
	}

	/**
	 * Dumps object content (public data and getters) through Reflection to a
	 * string, line-by-line.
	 *
	 * Private fields are not dumped.
	 *
	 * @param prefix
	 *            To be appended before each line.
	 * @param object
	 *            The monster.
	 */
	public static String object(String prefix, Object object) {
		StringBuilder sb = new StringBuilder();
		object(sb, prefix, object);
		return sb.toString();
	}

	public static void object(StringBuilder sb, String prefix, Object object) {
		new Dump(sb).d(prefix, object);
	}

	private void d(String prefix, Object object) {
		if (object != null)
			d(prefix, object, object.getClass());
		else
			d(prefix, object, void.class);
	}

	private void d(String prefix, Object object, Class<?> clazz) {
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
		}

		try {
			if (clazz == String.class)
				sb.append(" \"" + object + "\"");

			if (!Collection.class.isAssignableFrom(clazz))
				sb.append(" " + object);

			sb.append(" [" + clazz.getSimpleName() + "]\n");

			// simple listings for simple classes
			if (isSimpleType(clazz))
				return;

			for (Field field : clazz.getFields())
				if (!Modifier.isStatic(field.getModifiers()))
					try {
						String name = field.getName();
						Object o = field.get(object);
						Class<?> type = field.getType();
						if (isSimpleType(type))
							d(prefix + "." + name, o, type);
						else
							d(prefix + "." + name, o);
					} catch (Throwable ex) {
						sb.append(prefix + "." + field.getName());
						sb.append(" caught " + ex + "\n");
					}

			Set<String> displayedMethod = new HashSet<>();

			for (Method method : clazz.getMethods()) {
				String name = method.getName();
				try {
					if (name.startsWith("get") //
							&& method.getParameterTypes().length == 0 //
							&& !displayedMethod.contains(name)) {
						Object o = method.invoke(object);
						if (!(o instanceof Class<?>))
							d(prefix + "." + name + "()", o);

						// do not display same method of different base
						// classes
						displayedMethod.add(name);
					}
				} catch (Throwable ex) {
					sb.append(prefix + "." + name + "()");
					sb.append(" caught " + ex + "\n");
				}
			}

			int count = 0;

			if (clazz.isArray()) {
				Class<?> componentType = clazz.getComponentType();
				if (componentType.isPrimitive())
					for (int i = 0; i < Array.getLength(object); i++)
						d(prefix + "[" + count++ + "]", Array.get(object, i));
				else if (Object.class.isAssignableFrom(componentType))
					for (Object o1 : (Object[]) object)
						d(prefix + "[" + count++ + "]", o1);
			}

			if (Collection.class.isAssignableFrom(clazz))
				for (Object o1 : (Collection<?>) object)
					d(prefix + "[" + count++ + "]", o1);
			else if (Map.class.isAssignableFrom(clazz))
				for (Entry<?, ?> entry : ((Map<?, ?>) object).entrySet()) {
					Object key = entry.getKey(), value = entry.getValue();
					d(prefix + "[" + count + "].getKey()", key);
					d(prefix + "[" + count + "].getValue()", value);
					count++;
				}
		} finally {
			dumpedIds.remove(id);
		}
	}

	/**
	 * Types that do not require per-member dump.
	 */
	private static boolean isSimpleType(Class<?> clazz) {
		return clazz.isPrimitive() //
				|| clazz == Class.class //
				|| clazz == Date.class //
				|| clazz == String.class //
				|| clazz == Timestamp.class;
	}

}
