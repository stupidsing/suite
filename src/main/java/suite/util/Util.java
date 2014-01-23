package suite.util;

import java.io.Closeable;
import java.io.IOException;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import suite.util.FunUtil.Source;

public class Util {

	@SafeVarargs
	public static <T> List<T> add(List<T>... lists) {
		List<T> resultList = new ArrayList<>();
		for (List<T> list : lists)
			resultList.addAll(list);
		return resultList;
	}

	public static char charAt(String s, int pos) {
		if (pos < 0)
			pos += s.length();
		return s.charAt(pos);
	}

	public static Iterable<Character> chars(final String s) {
		return iter(new Iterator<Character>() {
			private int index = 0;

			public boolean hasNext() {
				return index < s.length();
			}

			public Character next() {
				return s.charAt(index++);
			}

			public void remove() {
				throw new UnsupportedOperationException();
			}
		});
	}

	public static Class<?> clazz(Object object) {
		return object != null ? object.getClass() : null;
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

	public static <T> Collection<T> common(Collection<T> c0, Collection<T> c1) {
		Set<T> set = new HashSet<T>(c0);
		set.retainAll(c1);
		return set;
	}

	public static <T extends Comparable<T>> int compare(T t0, T t1) {
		if (t0 == null ^ t1 == null)
			return t0 != null ? 1 : -1;
		else
			return t0 != null ? t0.compareTo(t1) : 0;
	}

	public static long createDate(int year, int month, int day) {
		return createDate(year, month, day, 0, 0, 0);
	}

	public static long createDate(int year, int month, int day, int hour, int minute, int second) {
		Calendar cal = Calendar.getInstance();
		cal.clear();
		cal.set(year, Calendar.JANUARY + month - 1, day, hour, minute, second);
		return cal.getTimeInMillis();
	}

	public static ThreadPoolExecutor createExecutor() {
		return new ThreadPoolExecutor(8, 32, 10, TimeUnit.SECONDS, new ArrayBlockingQueue<Runnable>(256));
	}

	public static Class<?> currentClass() {
		try {
			return Class.forName(getStackTrace(3).getClassName());
		} catch (ClassNotFoundException ex) {
			ex.printStackTrace();
			return null;
		}
	}

	public static String currentMethod() {
		return getStackTrace(3).getMethodName();
	}

	public static String currentPackage() {
		String cls = getStackTrace(3).getClassName();
		int pos = cls.lastIndexOf(".");
		return cls.substring(0, pos);
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
		new DumpUtil(sb).d("", object);
		LogUtil.info(sb.toString());
	}

	/**
	 * Imposes typing on equal() function to avoid errors. Also handle null
	 * value comparisons.
	 */
	public static <T> boolean equals(T t0, T t1) {
		if (t0 == null ^ t1 == null)
			return false;
		else
			return t0 == null || t0.equals(t1);
	}

	public static <T> T first(Collection<T> c) {
		return !c.isEmpty() ? c.iterator().next() : null;
	}

	public static StackTraceElement getStackTrace(int n) {
		return Thread.currentThread().getStackTrace()[n];
	}

	public static <T> int hashCode(T t) {
		return t != null ? t.hashCode() : 0;
	}

	public static boolean isBlank(String s) {
		boolean isBlank = true;
		if (s != null)
			for (char c : s.toCharArray())
				isBlank &= Character.isWhitespace(c);
		return isBlank;
	}

	public static boolean isNotBlank(String s) {
		return !isBlank(s);
	}

	public static <T> Iterable<T> iter(final Iterator<T> iter) {
		return new Iterable<T>() {
			public Iterator<T> iterator() {
				return iter;
			}
		};
	}

	public static <T> T last(List<T> c) {
		return !c.isEmpty() ? c.get(c.size() - 1) : null;
	}

	public static <T> List<T> left(List<T> list, int pos) {
		int size = list.size();
		if (pos < 0)
			pos += size;
		return list.subList(0, Math.min(size, pos));
	}

	public static <T> List<T> right(List<T> list, int pos) {
		int size = list.size();
		if (pos < 0)
			pos += size;
		return list.subList(Math.min(size, pos), size);
	}

	public static void sleepQuietly(long time) {
		try {
			Thread.sleep(time);
		} catch (InterruptedException ex) {
			LogUtil.error(ex);
			Thread.currentThread().interrupt();
		}
	}

	public static <T extends Comparable<? super T>> List<T> sort(Collection<T> list) {
		List<T> list1 = new ArrayList<>(list);
		Collections.sort(list1);
		return list1;
	}

	public static <T> List<T> sort(Collection<T> list, Comparator<? super T> comparator) {
		List<T> list1 = new ArrayList<>(list);
		Collections.sort(list1, comparator);
		return list1;
	}

	public static Pair<String, String> split2(String s, String delimiter) {
		int pos = s.indexOf(delimiter);
		if (pos >= 0)
			return Pair.create(s.substring(0, pos).trim(), s.substring(pos + delimiter.length()).trim());
		else
			return Pair.create(s, "");
	}

	public static Iterable<String> splitn(final String s, final String delimiter) {
		return FunUtil.iter(new Source<String>() {
			private int pos = 0;

			public String source() {
				String splitted;
				int pos1;

				if ((pos1 = s.indexOf(delimiter, pos)) >= 0) {
					splitted = s.substring(pos, pos1).trim();
					pos = pos1 + delimiter.length();
				} else if (!(splitted = s.substring(pos).trim()).isEmpty())
					pos = s.length();
				else
					splitted = null;

				return splitted;
			}
		});
	}

	public static String substr(String s, int start, int end) {
		int length = s.length();
		if (start < 0)
			start += length;
		if (end < start)
			end += length;
		end = Math.min(length, end);
		return s.substring(start, end);
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

}
