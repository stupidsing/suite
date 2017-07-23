package suite.util;

import java.io.InputStream;
import java.io.Reader;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.log4j.Level;

import suite.os.LogUtil;
import suite.primitive.IntMutable;
import suite.primitive.IntPrimitives.IntSource;
import suite.sample.Profiler;
import suite.util.Thread_.RunnableEx;

public class Util {

	private static AtomicInteger counter = new AtomicInteger();

	public enum RunOption {
		RUN____, PROFILE, TIME___,
	};

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

		T[] result = Array_.newArray(clazz, size);
		int i = 0;

		for (T[] list : lists) {
			int length = list.length;
			Array_.copy(list, 0, result, i, length);
			i += length;
		}
		return result;
	}

	public static void assert_(boolean b) {
		if (!b)
			throw new AssertionError();
	}

	/**
	 * Reads a line from a stream with a maximum line length limit. Removes carriage
	 * return if it is DOS-mode line feed (CR-LF). Unknown behaviour when dealing
	 * with non-ASCII encoding characters.
	 */
	public static String readLine(InputStream is) {
		return Rethrow.ex(() -> {
			StringBuilder sb = new StringBuilder();
			int c;
			while (0 <= (c = is.read()) && c != 10) {
				sb.append((char) c);
				if (65536 <= sb.length())
					throw new RuntimeException("line too long");
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
					throw new RuntimeException("line too long");
			}
			return 0 <= c ? strip(sb) : null;
		});
	}

	public static void run(RunnableEx runnableEx, RunOption runOption) {
	}

	public static void run(Class<? extends ExecutableProgram> clazz, String[] args) {
		run(clazz, args, RunOption.RUN____);
	}

	public static void run(Class<? extends ExecutableProgram> clazz, String[] args, RunOption runOption) {
		LogUtil.initLog4j(Level.INFO);
		IntMutable mutableCode = IntMutable.nil();

		Callable<Integer> runnableEx = () -> {
			try (ExecutableProgram main_ = clazz.newInstance()) {
				return main_.run(args) ? 0 : 1;
			} catch (ReflectiveOperationException ex) {
				LogUtil.fatal(ex);
				return 2;
			}
		};

		IntSource source = () -> {
			try {
				return runnableEx.call();
			} catch (Throwable ex) {
				LogUtil.fatal(ex);
				return 2;
			}
		};

		Runnable runnable = () -> mutableCode.set(source.source());

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

		int code = mutableCode.get();
		if (code != 0)
			System.exit(code);
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

}
