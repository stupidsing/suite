package org.util;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.Calendar;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

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

	public static boolean isBlank(String s) {
		boolean isBlank = true;
		if (s != null)
			for (char c : s.toCharArray())
				isBlank &= Character.isWhitespace(c);
		return isBlank;
	}

	public static long createDate(int year, int month, int day) {
		return createDate(year, month, day, 0, 0, 0);
	}

	public static long createDate(int year, int month, int day, int hour,
			int minute, int second) {
		Calendar cal = Calendar.getInstance();
		cal.clear();
		cal.set(year, month + Calendar.JANUARY - 1, day, hour, minute, second);
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
			return new Pair<>(t1, t2);
		}

		public boolean equals(Object o) {
			if (o instanceof Pair<?, ?>) {
				Pair<?, ?> t = (Pair<?, ?>) o;
				return Util.equals(t1, t.t1) && Util.equals(t2, t.t2);
			} else
				return false;
		}

		public int hashCode() {
			int h1 = t1 != null ? t1.hashCode() : 0;
			int h2 = t2 != null ? t2.hashCode() : 0;
			return h1 ^ h2;
		}

		public String toString() {
			return t1.toString() + ":" + t2.toString();
		}
	}

	public static void sleep(long time) {
		try {
			Thread.sleep(time);
		} catch (InterruptedException ex) {
			LogUtil.error(ex);
			Thread.currentThread().interrupt();
		}
	}

	public static void wait(Object object) {
		wait(object, 0);
	}

	public static void wait(Object object, int timeOut) {
		try {
			object.wait(timeOut);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}
	}

	public static ThreadPoolExecutor createExecutor() {
		return new ThreadPoolExecutor(8, 32 //
				, 10, TimeUnit.SECONDS //
				, new ArrayBlockingQueue<Runnable>(256));
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

	public static <E> void truncate(List<E> list, int n) {
		int size = list.size();
		while (size > n)
			list.remove(--size);
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
		StringBuilder sb = new StringBuilder();
		sb.append("Dumping ");
		sb.append(name);
		DumpUtil.dump("", object, sb);
		LogUtil.info(sb.toString());
	}

	/**
	 * Imposes typing on equal() function to avoid errors. Also handle null
	 * value comparisons.
	 */
	public static <T> boolean equals(T t1, T t2) {
		if (t1 == null ^ t2 == null)
			return false;
		else
			return t1 != null ? t1.equals(t2) : true;
	}

	public static <T extends Comparable<T>> int compare(T t1, T t2) {
		if (t1 == null ^ t2 == null)
			return t1 != null ? 1 : -1;
		else
			return t1 != null ? t1.compareTo(t2) : 0;
	}

	public static <T> int hashCode(T t) {
		return t != null ? t.hashCode() : 0;
	}

	/**
	 * Clones slowly by serializing and de-serializing.
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

	public static <T> void copyArray(T from[], int fromIndex //
			, T to[], int toIndex, int size) {
		if (size != 0)
			System.arraycopy(from, fromIndex, to, toIndex, size);
	}

	public static <T> void copyPrimitiveArray(Object from, int fromIndex //
			, Object to, int toIndex, int size) {
		if (size != 0)
			System.arraycopy(from, fromIndex, to, toIndex, size);
	}

	public static String substr(String s, int start, int end) {
		int length = s.length();
		if (start < 0)
			start += length;
		if (end <= 0)
			end += length;
		return s.substring(start, end);
	}

	public static void closeQuietly(Closeable o) {
		if (o != null)
			try {
				o.close();
			} catch (IOException ex) {
				throw new RuntimeException(ex);
			}
	}

	public static void closeQuietly(Socket o) {
		if (o != null)
			try {
				o.close();
			} catch (IOException ex) {
				throw new RuntimeException(ex);
			}
	}

}
