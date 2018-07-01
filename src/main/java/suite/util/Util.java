package suite.util;

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
import suite.Defaults;
import suite.streamlet.Read;
import suite.util.FunUtil.Fun;
import suite.util.FunUtil.Sink;
import suite.util.FunUtil.Source;
import suite.util.FunUtil2.Fun2;

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
			return Rethrow.ex(() -> clazz.getMethod("test", Object.class, Object.class));
		else if (clazz == Fun.class || clazz == Function.class)
			return Rethrow.ex(() -> clazz.getMethod("apply", Object.class));
		else if (clazz == Fun2.class)
			return Rethrow.ex(() -> clazz.getMethod("apply", Object.class, Object.class));
		else if (clazz == Predicate.class)
			return Rethrow.ex(() -> clazz.getMethod("test", Object.class));
		else if (clazz == Sink.class)
			return Rethrow.ex(() -> clazz.getMethod("sink", Object.class));
		else if (clazz == Source.class)
			return Rethrow.ex(() -> clazz.getMethod("source"));
		else
			try {
				return Read //
						.from(clazz.getDeclaredMethods()) //
						.filter(method -> !method.isDefault() && !method.isSynthetic() && !Modifier.isStatic(method.getModifiers())) //
						.uniqueResult();
			} catch (Exception ex) {
				return Fail.t("for " + clazz, ex);
			}
	}

	/**
	 * Reads a line from a stream with a maximum line length limit. Removes carriage
	 * return if it is DOS-mode line feed (CR-LF). Unknown behaviour when dealing
	 * with non-ASCII encoding characters.
	 */
	public static String readLine(InputStream is) {
		return Rethrow.ex(() -> {
			var sb = new StringBuilder();
			int c;
			while (0 <= (c = is.read()) && c != 10) {
				sb.append((char) c);
				if (Defaults.bufferLimit <= sb.length())
					Fail.t("line too long");
			}
			return 0 <= c ? strip(sb) : null;
		});
	}

	public static String readLine(Reader reader) {
		return Rethrow.ex(() -> {
			var sb = new StringBuilder();
			int c;
			while (0 <= (c = reader.read()) && c != 10) {
				sb.append((char) c);
				if (Defaults.bufferLimit <= sb.length())
					Fail.t("line too long");
			}
			return 0 <= c ? strip(sb) : null;
		});
	}

	public static int temp() {
		return counter.getAndIncrement();
	}

	private static String strip(StringBuilder sb) {
		var length = sb.length();
		if (0 < length && sb.charAt(length - 1) == 13)
			sb.deleteCharAt(length - 1);
		return sb.toString();
	}

}
