package suite.util;

import java.io.InputStream;
import java.io.Reader;
import java.lang.reflect.Array;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.log4j.Level;

import suite.os.LogUtil;
import suite.sample.Profiler;

public class Util {

	private static AtomicInteger counter = new AtomicInteger();

	public enum RunOption {
		RUN____, PROFILE, TIME___,
	};

	public interface RunnableEx {
		public void run() throws Exception;
	}

	public static abstract class ExecutableProgram implements AutoCloseable {
		protected abstract boolean run(String[] args) throws Exception;

		public void close() {
		}
	}

	@SafeVarargs
	public static <T> T[] add(Class<T> clazz, T[]... lists) {
		int size = 0;

		for (T[] list : lists)
			size += list.length;

		T[] result = Util.newArray(clazz, size);
		int i = 0;

		for (T[] list : lists) {
			int length = list.length;
			Copy.array(list, 0, result, i, length);
			i += length;
		}
		return result;
	}

	public static void assert_(boolean b) {
		if (!b)
			throw new AssertionError();
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

	public static StackTraceElement getStackTrace(int n) {
		return Thread.currentThread().getStackTrace()[n];
	}

	@SuppressWarnings("unchecked")
	public static <T> T[] newArray(Class<T> clazz, int dim) {
		return (T[]) Array.newInstance(clazz, dim);
	}

	public static long newDate(int year, int month, int day) {
		return newDate(year, month, day, 0, 0, 0);
	}

	public static long newDate(int year, int month, int day, int hour, int minute, int second) {
		return ZonedDateTime.of(year, month, day, hour, minute, second, 0, ZoneId.systemDefault()).toEpochSecond() * 1000l;
	}

	public static ThreadPoolExecutor newExecutor() {
		return newExecutor(8, 32);
	}

	public static ThreadPoolExecutor newExecutorByProcessors() {
		int nProcessors = Runtime.getRuntime().availableProcessors();
		return newExecutor(nProcessors, nProcessors);
	}

	public static Thread newThread(RunnableEx runnable) {
		return new Thread() {
			public void run() {
				try {
					runnable.run();
				} catch (Exception ex) {
					LogUtil.error(ex);
				}
			}
		};
	}

	/**
	 * Reads a line from a stream with a maximum line length limit. Removes
	 * carriage return if it is DOS-mode line feed (CR-LF). Unknown behaviour
	 * when dealing with non-ASCII encoding characters.
	 */
	public static String readLine(InputStream is) {
		return Rethrow.ex(() -> {
			StringBuilder sb = new StringBuilder();
			int c;
			while (0 <= (c = is.read()) && c != 10) {
				sb.append((char) c);
				if (65536 <= sb.length())
					throw new RuntimeException("Line too long");
			}
			return 0 <= c ? strip(sb) : null;
		});
	}

	public static String readLine(Reader reader) {
		return Rethrow.ex(() -> {
			StringBuilder sb = new StringBuilder();
			int c;
			while (0 <= (c = reader.read()) && c != 10) {
				sb.append((char) c);
				if (65536 <= sb.length())
					throw new RuntimeException("Line too long");
			}
			return 0 <= c ? strip(sb) : null;
		});
	}

	public static void run(Class<? extends ExecutableProgram> clazz, String[] args) {
		run(clazz, args, RunOption.RUN____);
	}

	public static void run(Class<? extends ExecutableProgram> clazz, String[] args, RunOption runOption) {
		LogUtil.initLog4j(Level.INFO);
		Runnable runnable;
		int[] code = new int[1];

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
				new Profiler().profile(runnable);
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

	@SafeVarargs
	public static <T> Set<T> set(T... ts) {
		Set<T> set = new HashSet<>();
		for (T t : ts)
			set.add(t);
		return set;
	}

	public static void sleepQuietly(long time) {
		if (0 < time)
			try {
				Thread.sleep(time);
			} catch (InterruptedException ex) {
				LogUtil.error(ex);
			}
	}

	public static void startJoin(Collection<Thread> threads) {
		for (Thread thread : threads)
			thread.start();

		for (Thread thread : threads)
			try {
				thread.join();
			} catch (InterruptedException ex) {
				LogUtil.error(ex);
			}
	}

	public static Thread startThread(RunnableEx runnable) {
		Thread thread = newThread(runnable);
		thread.start();
		return thread;
	}

	public static int temp() {
		return counter.getAndIncrement();
	}

	private static String strip(StringBuilder sb) {
		int length = sb.length();
		if (0 < length && sb.charAt(length - 1) == 13)
			sb.deleteCharAt(length - 1);
		return sb.toString();
	}

	private static ThreadPoolExecutor newExecutor(int corePoolSize, int maxPoolSize) {
		return new ThreadPoolExecutor(corePoolSize, maxPoolSize, 10, TimeUnit.SECONDS, new ArrayBlockingQueue<>(256));
	}

}
