package suite.inspect;

import java.lang.reflect.Array;
import java.util.Collection;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;

import suite.adt.pair.Pair;
import suite.node.util.Singleton;
import suite.os.LogUtil;
import suite.streamlet.FunUtil.Sink;
import suite.streamlet.Streamlet;
import suite.util.MapInterface;
import suite.util.MapObject_;
import suite.util.Switch;
import suite.util.Thread_;
import suite.util.Util;

public class Dump {

	private static Inspect inspect = Singleton.me.inspect;

	public static <T> T t(T t) {
		details(t);
		return t;
	}

	public static void line(Object node) {
		LogUtil.info(toLine(node));
	}

	public static String toLine(Object node) {
		var dumpedObjects = new IdentityHashMap<Object, Object>();
		var sb = new StringBuilder();
		Sink<String> sink = sb::append;

		new Object() {
			private void d(Object object) {
				if (object == null)
					sink.sink("null");
				else if (dumpedObjects.put(object, true) == null)
					try {
						d_(object);
					} finally {
						dumpedObjects.remove(object);
					}
				else
					sink.sink("(recursed)");
			}

			private void d_(Object object) {
				var clazz = object.getClass();

				if (clazz.isArray()) {
					sink.sink("[");
					for (var i = 0; i < Array.getLength(object); i++) {
						d(Array.get(object, i));
						sink.sink(",");
					}
					sink.sink("]");
				} else if (Util.isSimple(clazz))
					sink.sink(object.toString());
				else
					new Switch<Object>(object //
					).doIf(Collection.class, collection -> {
						sink.sink("[");
						for (var object1 : collection) {
							d(object1);
							sink.sink(",");
						}
						sink.sink("]");
					}).doIf(Map.class, map -> {
						sink.sink("{");
						for (var e : ((Map<?, ?>) object).entrySet()) {
							d(e.getKey());
							sink.sink(":");
							d(e.getValue());
							sink.sink(",");
						}
						sink.sink("}");
					}).doIf(MapInterface.class, mi -> {
						sink.sink(mi.getClass().getSimpleName());
						sink.sink("{");
						for (var object1 : MapObject_.list(object)) {
							d(object1);
							sink.sink(",");
						}
						sink.sink("}");
					}).doIf(Pair.class, pair -> {
						sink.sink("<");
						d(pair.t0);
						sink.sink("|");
						d(pair.t1);
						sink.sink(">");
					}).doIf(Object.class, o -> {
						sink.sink(o.getClass().getSimpleName());
						sink.sink("{");
						for (var pair : readers(object)) {
							Object value;
							try {
								value = pair.t1.call();
							} catch (Throwable ex) {
								value = "<" + ex.getClass() + ">";
							}

							if (value != null) {
								sink.sink(pair.t0 + ":");
								d(value);
								sink.sink(",");
							}
						}
						sink.sink("}");
					}).nonNullResult();
			}
		}.d(node);

		return sb.toString();
	}

	/**
	 * Dumps object content (public data and getters) through Reflection to a
	 * log4j.
	 */
	public static void details(Object object) {
		var trace = Thread_.getStackTrace(3);
		details(trace.getClassName() + "." + trace.getMethodName(), object);
	}

	/**
	 * Dumps object content (public data and getters) through Reflection to a
	 * log4j, with a descriptive name which you gave.
	 */
	public static void details(String name, Object object) {
		var sb = new StringBuilder();
		sb.append("Dumping ");
		sb.append(name);
		Dump.toDetails("", object, sb);
		LogUtil.info(sb.toString());
	}

	public static String object(Object object) {
		return toDetails("", object);
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
	public static String toDetails(String prefix, Object object) {
		var sb = new StringBuilder();
		toDetails(prefix, object, sb);
		return sb.toString();
	}

	public static void toDetails(String prefix, Object object, StringBuilder sb) {
		toDetails(prefix, object, sb::append);
	}

	private static void toDetails(String prefix, Object object, Sink<String> sink) {
		var dumpedObjects = new IdentityHashMap<Object, Object>();

		new Object() {
			private void d(String prefix, Object object) {
				d(prefix, object, object != null ? object.getClass() : void.class);
			}

			private void d(String prefix, Object object, Class<?> clazz) {
				sink.sink(prefix);
				sink.sink(" =");

				if (object == null)
					sink.sink(" null\n");
				else if (dumpedObjects.put(object, true) == null)
					try {
						d_(prefix, object, clazz);
					} finally {
						dumpedObjects.remove(object);
					}
				else
					sink.sink(" <<recursed>>");
			}

			private void d_(String prefix, Object object, Class<?> clazz) {
				if (clazz == String.class)
					sink.sink(" \"" + object + "\"");

				if (!Collection.class.isAssignableFrom(clazz))
					sink.sink(" " + object);

				sink.sink(" [" + clazz.getSimpleName() + "]\n");

				var count = 0;

				// simple listings for simple classes
				if (Util.isSimple(clazz))
					;
				else if (clazz.isArray())
					for (var i = 0; i < Array.getLength(object); i++)
						d(prefix + "[" + count++ + "]", Array.get(object, i));
				else if (Collection.class.isAssignableFrom(clazz))
					for (var o1 : (Collection<?>) object)
						d(prefix + "[" + count++ + "]", o1);
				else if (Map.class.isAssignableFrom(clazz))
					for (var e : ((Map<?, ?>) object).entrySet()) {
						Object key = e.getKey(), value = e.getValue();
						d(prefix + "[" + count + "].getKey()", key);
						d(prefix + "[" + count + "].getValue()", value);
						count++;
					}
				else if (MapInterface.class.isAssignableFrom(clazz)) {
					var i = 0;
					for (var object1 : MapObject_.list(object))
						d(prefix + "." + i++, object1);
				} else if (Pair.class.isAssignableFrom(clazz)) {
					var pair = (Pair<?, ?>) object;
					d(prefix + ".fst", pair.t0);
					d(prefix + ".snd", pair.t1);
				} else
					for (var pair : readers(object)) {
						var k = prefix + "." + pair.t0;

						try {
							d(k, pair.t1.call());
						} catch (Throwable ex) {
							sink.sink(k + "()");
							sink.sink(" caught " + ex + "\n");
						}
					}
			}
		}.d(prefix, object);
	}

	private static List<Pair<String, Callable<Object>>> readers(Object object) {
		var clazz = object.getClass();

		return Streamlet.concat( //
				inspect.fields(clazz).map(f -> Pair.<String, Callable<Object>> of(f.getName(), () -> f.get(object))), //
				inspect.getters(clazz).map(m -> Pair.<String, Callable<Object>> of(m.getName() + "()", () -> {
					var o_ = m.invoke(object);
					return !(o_ instanceof Class<?>) ? o_ : null;
				}))) //
				.toList();
	}

}
