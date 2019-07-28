package suite.util;

import static suite.util.Fail.fail;
import static suite.util.Rethrow.ex;

import java.io.InputStream;
import java.io.Reader;
import java.lang.reflect.Method;
import java.sql.Timestamp;
import java.util.Date;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiPredicate;
import java.util.function.Function;
import java.util.function.Predicate;

import javassist.Modifier;
import suite.cfg.Defaults;
import suite.streamlet.FunUtil.Fun;
import suite.streamlet.FunUtil.Sink;
import suite.streamlet.FunUtil.Source;
import suite.streamlet.FunUtil2.Fun2;
import suite.streamlet.Read;

public class Util {

	private static AtomicInteger counter = new AtomicInteger();

	public static void assert_(boolean b) {
		if (!b)
			throw new AssertionError();
	}

	public static boolean isSimple(Class<?> clazz) {
		return clazz.isEnum() //
				|| clazz.isPrimitive() //
				|| clazz == Boolean.class //
				|| clazz == Class.class //
				|| clazz == Date.class //
				|| clazz == String.class //
				|| clazz == Timestamp.class //
				|| Number.class.isAssignableFrom(clazz);
	}

	public static Method methodOf(Class<?> clazz) {
		if (clazz == BiPredicate.class)
			return ex(() -> clazz.getMethod("test", Object.class, Object.class));
		else if (clazz == Fun.class || clazz == Function.class)
			return ex(() -> clazz.getMethod("apply", Object.class));
		else if (clazz == Fun2.class)
			return ex(() -> clazz.getMethod("apply", Object.class, Object.class));
		else if (clazz == Predicate.class)
			return ex(() -> clazz.getMethod("test", Object.class));
		else if (clazz == Sink.class)
			return ex(() -> clazz.getMethod("sink", Object.class));
		else if (clazz == Source.class)
			return ex(() -> clazz.getMethod("g"));
		else
			try {
				return Read //
						.from(clazz.getDeclaredMethods()) //
						.filter(m -> !m.isDefault() && !m.isSynthetic() && !Modifier.isStatic(m.getModifiers())) //
						.uniqueResult();
			} catch (Exception ex) {
				return fail("for " + clazz, ex);
			}
	}

	/**
	 * Reads a line from a stream with a maximum line length limit. Removes carriage
	 * return if it is DOS-mode line feed (CR-LF). Unknown behaviour when dealing
	 * with non-ASCII encoding characters.
	 */
	public static String readLine(InputStream is) {
		return ex(() -> {
			var sb = new StringBuilder();
			int c;
			while (0 <= (c = is.read()) && c != 10) {
				sb.append((char) c);
				if (Defaults.bufferLimit <= sb.length())
					fail("line too long");
			}
			return 0 <= c ? strip(sb) : null;
		});
	}

	public static String readLine(Reader reader) {
		return ex(() -> {
			var sb = new StringBuilder();
			int c;
			while (0 <= (c = reader.read()) && c != 10) {
				sb.append((char) c);
				if (Defaults.bufferLimit <= sb.length())
					fail("line too long");
			}
			return 0 <= c ? strip(sb) : null;
		});
	}

	public static int temp() {
		return counter.getAndIncrement();
	}

	public static <T extends Exception, R> R throwSneakly(Exception ex) throws T {
		@SuppressWarnings("unchecked")
		var t = (T) ex;
		throw t;
	}

	private static String strip(StringBuilder sb) {
		var length = sb.length();
		if (0 < length && sb.charAt(length - 1) == 13)
			sb.deleteCharAt(length - 1);
		return sb.toString();
	}

}
