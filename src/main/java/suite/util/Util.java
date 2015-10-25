package suite.util;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Array;
import java.net.Socket;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Level;

import suite.adt.Pair;
import suite.inspect.Dump;
import suite.os.LogUtil;
import suite.sample.Profiler;

public class Util {

	public enum RunOption {
		RUN____, PROFILE, TIME___,
	};

	public interface RunnableEx {
		public void run() throws Exception;
	}

	public static abstract class ExecutableProgram implements AutoCloseable {
		protected abstract boolean run(String args[]) throws Exception;

		public void close() {
		}
	}

	@SafeVarargs
	public static <T> List<T> add(List<T>... lists) {
		List<T> resultList = new ArrayList<>();
		for (List<T> list : lists)
			resultList.addAll(list);
		return resultList;
	}

	@SafeVarargs
	public static <T> Set<T> add(Set<T>... sets) {
		Set<T> resultSet = new HashSet<>();
		for (Set<T> set : sets)
			resultSet.addAll(set);
		return resultSet;
	}

	public static char charAt(String s, int pos) {
		if (pos < 0)
			pos += s.length();
		return s.charAt(pos);
	}

	public static Iterable<Character> chars(CharSequence s) {
		return () -> new Iterator<Character>() {
			private int index = 0;

			public boolean hasNext() {
				return index < s.length();
			}

			public Character next() {
				return s.charAt(index++);
			}
		};
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

	public static <T extends Comparable<? super T>> Comparator<T> comparator() {
		return Util::compare;
	}

	public static <T extends Comparable<? super T>> int compare(T t0, T t1) {
		if (t0 == null ^ t1 == null)
			return t0 != null ? 1 : -1;
		else
			return t0 != null ? t0.compareTo(t1) : 0;
	}

	public static long createDate(int year, int month, int day) {
		return createDate(year, month, day, 0, 0, 0);
	}

	public static long createDate(int year, int month, int day, int hour, int minute, int second) {
		return ZonedDateTime.of(year, month, day, hour, minute, second, 0, ZoneId.systemDefault()).toEpochSecond() * 1000l;
	}

	public static ThreadPoolExecutor createExecutor() {
		return createExecutor(8, 32);
	}

	public static ThreadPoolExecutor createExecutorByProcessors() {
		int nProcessors = Runtime.getRuntime().availableProcessors();
		return createExecutor(nProcessors, nProcessors);
	}

	private static ThreadPoolExecutor createExecutor(int corePoolSize, int maxPoolSize) {
		return new ThreadPoolExecutor(corePoolSize, maxPoolSize, 10, TimeUnit.SECONDS, new ArrayBlockingQueue<>(256));
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
		Dump.object(sb, "", object);
		LogUtil.info(sb.toString());
	}

	public static <T> T first(Collection<T> c) {
		return !c.isEmpty() ? c.iterator().next() : null;
	}

	public static StackTraceElement getStackTrace(int n) {
		return Thread.currentThread().getStackTrace()[n];
	}

	public static boolean isBlank(String s) {
		boolean isBlank = true;
		if (s != null)
			for (char c : chars(s))
				isBlank &= Character.isWhitespace(c);
		return isBlank;
	}

	public static boolean isNotBlank(String s) {
		return !isBlank(s);
	}

	public static <T> T last(List<T> c) {
		return !c.isEmpty() ? c.get(c.size() - 1) : null;
	}

	public static <T> List<T> left(List<T> list, int pos) {
		int size = list.size();
		if (pos < 0)
			pos += size;
		return list.subList(0, Math.min(pos, size));
	}

	public static <T> T[] newArray(Class<T> clazz, int dim) {
		@SuppressWarnings("unchecked")
		T[] ts = (T[]) Array.newInstance(clazz, dim);
		return ts;
	}

	public static <T> Comparator<T> nullsFirst(Comparator<T> cmp0) {
		return (key0, key1) -> {
			boolean b0 = key0 != null;
			boolean b1 = key1 != null;

			if (b0 && b1)
				return cmp0.compare(key0, key1);
			else
				return b0 ? 1 : b1 ? -1 : 0;
		};
	}

	/**
	 * Reads a line from a stream with a maximum line length limit. Removes
	 * carriage return if it is DOS-mode line feed (CR-LF). Unknown behaviour
	 * when dealing with non-ASCII encoding characters.
	 */
	public static String readLine(InputStream is) throws IOException {
		StringBuilder sb = new StringBuilder();
		int c;

		while ((c = is.read()) != -1 && c != 10) {
			sb.append((char) c);
			if (sb.length() > 65536)
				throw new RuntimeException("Line too long");
		}

		int length = sb.length();

		if (sb.charAt(length - 1) == 13)
			sb.deleteCharAt(length - 1);

		return sb.toString();
	}

	public static <T> List<T> reverse(List<T> list0) {
		List<T> list1 = new ArrayList<>();
		for (int i = list0.size() - 1; i >= 0; i--)
			list1.add(list0.get(i));
		return list1;
	}

	public static <T> List<T> right(List<T> list, int pos) {
		int size = list.size();
		if (pos < 0)
			pos += size;
		return list.subList(Math.min(pos, size), size);
	}

	public static void run(Class<? extends ExecutableProgram> clazz, String args[]) {
		run(clazz, args, RunOption.RUN____);
	}

	public static void run(Class<? extends ExecutableProgram> clazz, String args[], RunOption runOption) {
		LogUtil.initLog4j(Level.INFO);
		Runnable runnable;
		int code[] = new int[1];

		try (ExecutableProgram main_ = clazz.newInstance()) {
			runnable = () -> {
				try {
					code[0] = main_.run(args) ? 0 : 1;
				} catch (Throwable ex) {
					LogUtil.fatal(ex);
					code[0] = 2;
				}
			};

			switch (runOption) {
			case PROFILE:
				System.out.println(new Profiler().profile(runnable));
				break;
			case RUN____:
				runnable.run();
				break;
			case TIME___:
				LogUtil.duration(clazz.getSimpleName(), () -> {
					runnable.run();
					return Boolean.TRUE;
				});
			}
		} catch (ReflectiveOperationException ex) {
			LogUtil.fatal(ex);
			code[0] = 2;
		}

		System.exit(code[0]);
	}

	public static void sleepQuietly(long time) {
		try {
			Thread.sleep(time);
		} catch (InterruptedException ex) {
			LogUtil.error(ex);
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
			return Pair.of(s.substring(0, pos).trim(), s.substring(pos + delimiter.length()).trim());
		else
			return Pair.of(s.trim(), "");
	}

	public static <T> List<List<T>> splitn(List<T> list, int n) {
		int s = 0;
		List<List<T>> subsets = new ArrayList<>();
		while (s < list.size()) {
			int s1 = Math.min(s + n, list.size());
			subsets.add(list.subList(s, s1));
			s = s1;
		}
		return subsets;
	}

	public static Thread startThread(RunnableEx runnable) {
		Thread thread = new Thread() {
			public void run() {
				try {
					runnable.run();
				} catch (Exception ex) {
					LogUtil.error(ex);
				}
			}
		};
		thread.start();
		return thread;
	}

	public static boolean stringEquals(String s0, String s1) {
		return Objects.equals(s0, s1);
	}

	public static String substr(String s, int start, int end) {
		int length = s.length();
		if (start < 0)
			start += length;
		if (end <= 0)
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
		}
	}

}
