package org.util;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;
import java.util.Vector;
import java.util.Map.Entry;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import sun.reflect.Reflection;

public class Util {

	public static String currentPackage() {
		String cls = getStackTrace(3).getClassName();
		int pos = cls.lastIndexOf(".");
		return cls.substring(0, pos);
	}

	public static Class<?> currentClass() {
		return Reflection.getCallerClass(2);
		// try { return Class.forName(getStackTrace(3).getClassName()); } catch
		// (ClassNotFoundException ex) { ex.printStackTrace(); return null; }
	}

	public static String currentMethod() {
		return getStackTrace(3).getMethodName();
	}

	private static StackTraceElement getStackTrace(int n) {
		return Thread.currentThread().getStackTrace()[n];
	}

	// Allows generic-object creation with type parameter inductions

	public static <T> Stack<T> createStack() {
		return new Stack<T>();
	}

	public static <T> Vector<T> createVector() {
		return new Vector<T>();
	}

	public static <T> List<T> createList() {
		return new ArrayList<T>();
	}

	public static <T> List<T> createList(Collection<T> c) {
		return new ArrayList<T>(c);
	}

	public static <K, V> Map<K, V> createHashMap() {
		return new HashMap<K, V>();
	}

	public static long timeOfDay(int year, int month, int day) {
		Calendar cal = Calendar.getInstance();
		cal.clear();
		cal.set(year, month + (Calendar.JANUARY - 1), day);
		return cal.getTimeInMillis();
	}

	public static class Pair<T1, T2> {
		public T1 t1;
		public T2 t2;

		public Pair() {
		}

		public Pair(T1 t1, T2 t2) {
			this.t1 = t1;
			this.t2 = t2;
		}

		public static <T1, T2> Pair<T1, T2> create(T1 t1, T2 t2) {
			return new Pair<T1, T2>(t1, t2);
		}

		public boolean equals(Object o) {
			if (o instanceof Pair<?, ?>) {
				Pair<?, ?> t = (Pair<?, ?>) o;
				return Util.equals(t1, t.t1) && Util.equals(t2, t.t2);
			} else
				return false;
		}

		public int hashCode() {
			int h1 = (t1 != null) ? t1.hashCode() : 0;
			int h2 = (t2 != null) ? t2.hashCode() : 0;
			return h1 ^ h2;
		}

		public String toString() {
			return t1.toString() + ":" + t2.toString();
		}
	}

	public static void moveFile(File from, File to)
			throws FileNotFoundException, IOException {

		// Serious problem that renameTo do not work across partitions in Linux!
		// We fall back to copy the file if renameTo() failed.
		if (!from.renameTo(to)) {
			copyFile(from, to);
			from.delete();
		}
	}

	public static void copyFile(File from, File to)
			throws FileNotFoundException, IOException {
		InputStream in = new FileInputStream(from);
		OutputStream out = new FileOutputStream(to);
		// OutputStream out = new FileOutputStream(f2, true); // Append

		copyStream(in, out);
	}

	public static void copyStream(InputStream in, OutputStream out)
			throws IOException {
		try {
			int len;
			byte[] buf = new byte[1024];
			while ((len = in.read(buf)) > 0)
				out.write(buf, 0, len);
		} finally {
			in.close();
			out.close();
		}
	}

	public static void sleep(long time) {
		try {
			Thread.sleep(time);
		} catch (InterruptedException ex) {
			log.error("", ex);
		}
	}

	public static <E> E unique(List<E> list) {
		int size = list.size();
		if (size == 0)
			throw new RuntimeException("Result is empty");
		if (size > 1)
			throw new RuntimeException("Result not unique");
		else
			return list.get(0);
	}

	public interface Performer<E> {
		public void perform(E e);
	}

	public interface IoProcess<I, O, Ex extends Exception> {
		public O perform(I e) throws Ex;
	}

	/**
	 * Dumps object content (public data and getters) through Reflection to a
	 * log4j.
	 */
	public static void dump(Object object) {
		StackTraceElement trace = getStackTrace(3);
		dump(trace.getClassName() + "." + trace.getMethodName(), object);
	}

	/**
	 * Dumps object content (public data and getters) through Reflection to a
	 * log4j, with a descriptive name which you gave.
	 */
	public static void dump(String name, Object object) {
		StringBuffer sb = new StringBuffer();
		sb.append("Dumping ");
		sb.append(name);
		dump("", object, sb);
		log.info(sb.toString());
	}

	/**
	 * Imposes typing on equal() function to avoid errors. Also handle null
	 * value comparisons.
	 */
	public static <T> boolean equals(T t1, T t2) {
		if (t1 == null ^ t2 == null)
			return false;
		else
			return (t1 != null) ? t1.equals(t2) : true;
	}

	public static <T> int hashCode(T t) {
		return (t != null) ? t.hashCode() : 0;
	}

	/**
	 * Clones slowly using serialisation.
	 */
	public static <T> T copy(T clonee) throws IOException,
			ClassNotFoundException {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		ObjectOutputStream out = new ObjectOutputStream(baos);
		out.writeObject(clonee);
		out.flush();
		out.close();

		ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
		ObjectInputStream in = new ObjectInputStream(bais);
		@SuppressWarnings("unchecked")
		T cloned = (T) in.readObject();
		return cloned;
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
	public static void dump(String prefix, Object object, StringBuffer sb) {
		if (object != null)
			dump(prefix, object, object.getClass(), sb);
		else
			dump(prefix, object, void.class, sb);
	}

	public static void dump(String prefix, Object object, Class<?> clazz,
			StringBuffer sb) {
		sb.append(prefix);
		sb.append(" =");
		if (object == null) {
			sb.append(" null\n");
			return;
		}

		if (clazz == String.class)
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

		Set<String> displayedMethod = new HashSet<String>();
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

	private static Log log = LogFactory.getLog(Util.currentClass());

}
