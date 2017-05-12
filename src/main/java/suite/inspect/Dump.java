package suite.inspect;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import suite.jdk.gen.Type_;
import suite.node.util.Singleton;
import suite.os.LogUtil;
import suite.util.FunUtil.Sink;
import suite.util.Thread_;

public class Dump {

	private static Inspect inspect = Singleton.get().getInspect();

	private Set<Integer> dumpedIds = new HashSet<>();
	private Sink<String> sink;

	/**
	 * Dumps object content (public data and getters) through Reflection to a
	 * log4j.
	 */
	public static void out(Object object) {
		StackTraceElement trace = Thread_.getStackTrace(3);
		out(trace.getClassName() + "." + trace.getMethodName(), object);
	}

	/**
	 * Dumps object content (public data and getters) through Reflection to a
	 * log4j, with a descriptive name which you gave.
	 */
	public static void out(String name, Object object) {
		StringBuilder sb = new StringBuilder();
		sb.append("Dumping ");
		sb.append(name);
		Dump.object(sb, "", object);
		LogUtil.info(sb.toString());
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
		new Dump(sb::append).d(prefix, object);
	}

	public Dump(Sink<String> sink) {
		this.sink = sink;
	}

	private void d(String prefix, Object object) {
		if (object != null)
			d(prefix, object, object.getClass());
		else
			d(prefix, object, void.class);
	}

	private void d(String prefix, Object object, Class<?> clazz) {
		int id = System.identityHashCode(object);
		sink.sink(prefix);
		sink.sink(" =");

		if (object == null)
			sink.sink(" null\n");
		else if (dumpedIds.add(id))
			try {
				if (clazz == String.class)
					sink.sink(" \"" + object + "\"");

				if (!Collection.class.isAssignableFrom(clazz))
					sink.sink(" " + object);

				sink.sink(" [" + clazz.getSimpleName() + "]\n");

				// simple listings for simple classes
				if (Type_.isSimple(clazz))
					return;

				for (Field field : inspect.fields(clazz))
					try {
						String name = field.getName();
						Object o = field.get(object);
						Class<?> type = field.getType();
						if (Type_.isSimple(type))
							d(prefix + "." + name, o, type);
						else
							d(prefix + "." + name, o);
					} catch (Throwable ex) {
						sink.sink(prefix + "." + field.getName());
						sink.sink(" caught " + ex + "\n");
					}

				for (Method method : inspect.getters(clazz)) {
					String name = method.getName();
					try {
						Object o = method.invoke(object);
						if (!(o instanceof Class<?>))
							d(prefix + "." + name + "()", o);
					} catch (Throwable ex) {
						sink.sink(prefix + "." + name + "()");
						sink.sink(" caught " + ex + "\n");
					}
				}

				int count = 0;

				if (clazz.isArray())
					for (int i = 0; i < Array.getLength(object); i++)
						d(prefix + "[" + count++ + "]", Array.get(object, i));
				else if (Collection.class.isAssignableFrom(clazz))
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
		else
			sink.sink(" <<recursed>>");
	}

}
