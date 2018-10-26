package suite.os;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.InvocationTargetException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

import suite.inspect.Dump;
import suite.proxy.Intercept;
import suite.streamlet.FunUtil.Source;

public class Log_ {

	private static ThreadLocal<String> prefix = ThreadLocal.withInitial(() -> "");

	private static Out out = new Out() {
		private DateTimeFormatter yyyymmdd = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss", Locale.ENGLISH);

		public void info(String message) {
			log("[I]", message);
		}

		public void warn(String message) {
			log("[W]", message);
		}

		public void error(String message, Throwable th) {
			logException("[E]", message, th);
		}

		public void fatal(String message, Throwable th) {
			logException("[F]", message, th);
		}

		private void logException(String type, String message, Throwable th) {
			try (var sw = new StringWriter(); var pw = new PrintWriter(sw);) {
				th.printStackTrace(pw);
				log(type, (!message.isEmpty() ? message + ": " : "") + sw);
			} catch (IOException ex) {
				ex.printStackTrace();
			}
		}

		private void log(String type, String message) {
			System.out.println(current() + " " + type + " " + prefix.get() + message);
		}

		private String current() {
			return yyyymmdd.format(LocalDateTime.now());
		}
	};

	private interface Out {
		public void info(String message);

		public void warn(String message);

		public void error(String message, Throwable th);

		public void fatal(String message, Throwable th);
	}

	public static void initLogging(Object level) {
	}

	public static <T> T duration(String m, Source<T> source) {
		var tr = Stopwatch.of(source);
		Log_.info(m + " in " + tr.duration + "ms, GC occurred " + tr.nGcs + " times in " + tr.gcDuration + " ms");
		return tr.result;
	}

	public static <T> T prefix(String s, Source<T> source) {
		var prefix0 = prefix.get();
		prefix.set(prefix0 + s);
		try {
			return source.g();
		} finally {
			prefix.set(prefix0);
		}
	}

	public static <T> T log(String message, Source<T> source) {
		info("Enter " + message);
		try {
			return source.g();
		} finally {
			info("Exit " + message);
		}
	}

	public static void info(String message) {
		out.info(message);
	}

	public static void warn(String message) {
		out.warn(message);
	}

	public static void error(Throwable th) {
		error("", th);
	}

	public static void error(String message, Throwable th) {
		out.error(message, th);
	}

	public static void fatal(Throwable th) {
		fatal("", th);
	}

	public static void fatal(String message, Throwable th) {
		out.fatal(message, th);
	}

	public static <I> I proxy(Class<I> interface_, I object) {
		var tag = "[" + object.getClass().getSimpleName() + "] ";

		return Intercept.object(interface_, object, invocation -> (m, ps) -> {
			var methodName = m.getName();
			var prefix = methodName + "()\n";
			var sb = new StringBuilder();

			sb.append(prefix);

			if (ps != null)
				for (var i = 0; i < ps.length; i++)
					Dump.toDetails("p" + i, ps[i], sb);

			info(tag + sb.toString());

			try {
				var value = invocation.invoke(m, ps);
				var rd = Dump.toDetails("return", value);
				info(tag + prefix + rd);
				return value;
			} catch (InvocationTargetException ite) {
				var th = ite.getTargetException();
				error(tag + prefix, th);
				throw th instanceof Exception ? (Exception) th : ite;
			}
		});
	}

}
