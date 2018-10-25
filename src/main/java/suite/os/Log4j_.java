package suite.os;

import java.io.PrintWriter;
import java.lang.reflect.InvocationTargetException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.DailyRollingFileAppender;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;

import suite.cfg.Defaults;
import suite.inspect.Dump;
import suite.proxy.Intercept;
import suite.streamlet.FunUtil.Source;
import suite.util.Array_;

public class Log4j_ {

	private static int maxStackTraceLength = 99;
	private static ThreadLocal<String> prefix = ThreadLocal.withInitial(() -> "");

	private static Out out = new Out() {
		private Log log0 = LogFactory.getLog("suite");

		public void info(String message) {
			log0.info(message);
		}

		public void warn(String message) {
			log0.warn(message);
		}

		public void error(Throwable th) {
			var isTrimmed = trimStackTrace(th);
			log0.error(prefix.get() + (isTrimmed ? "(Trimmed)" : ""), th);
		}

		public void fatal(Throwable th) {
			var isTrimmed = trimStackTrace(th);
			log0.fatal(prefix.get() + (isTrimmed ? "(Trimmed)" : ""), th);
		}
	};

	private interface Out {
		public void info(String message);

		public void warn(String message);

		public void error(Throwable th);

		public void fatal(Throwable th);
	}

	static {
		initLog4j(Level.INFO);
	}

	public static void initLog4j(Level level) {
		var logDir = Defaults.tmp("logs");

		var layout = new PatternLayout("%d %-5p [%c{1}] %m%n");

		var console = new ConsoleAppender(layout);
		console.setWriter(new PrintWriter(System.err));
		console.activateOptions();

		var file = new DailyRollingFileAppender();
		file.setFile(logDir.resolve("suite.log").toString());
		file.setDatePattern("'.'yyyyMMdd");
		file.setLayout(layout);
		file.activateOptions();

		var logger = Logger.getRootLogger();
		logger.setLevel(level);
		logger.removeAllAppenders();
		logger.addAppender(console);
		logger.addAppender(file);
	}

	public static <T> T duration(String m, Source<T> source) {
		var tr = Stopwatch.of(source);
		Log4j_.info(m + " in " + tr.duration + "ms, GC occurred " + tr.nGcs + " times in " + tr.gcDuration + " ms");
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

	public static void info(String message) {
		out.info(prefix.get() + message);
	}

	public static void warn(String message) {
		out.warn(prefix.get() + message);
	}

	public static void error(Throwable th) {
		out.error(th);
	}

	public static void fatal(Throwable th) {
		out.fatal(th);
	}

	public static <T> T log(String message, Source<T> source) {
		info("Enter " + message);
		try {
			return source.g();
		} finally {
			info("Exit " + message);
		}
	}

	public static <I> I proxy(Class<I> interface_, I object) {
		var log = LogFactory.getLog(object.getClass());

		return Intercept.object(interface_, object, invocation -> (m, ps) -> {
			var methodName = m.getName();
			var prefix = methodName + "()\n";
			var sb = new StringBuilder();

			sb.append(prefix);

			if (ps != null)
				for (var i = 0; i < ps.length; i++)
					Dump.toDetails("p" + i, ps[i], sb);

			log.info(sb.toString());

			try {
				var value = invocation.invoke(m, ps);
				var rd = Dump.toDetails("return", value);
				log.info(prefix + rd);
				return value;
			} catch (InvocationTargetException ite) {
				var th = ite.getTargetException();
				var isTrimmed = trimStackTrace(th);
				log.error(prefix + (isTrimmed ? "(Trimmed)" : ""), th);
				throw th instanceof Exception ? (Exception) th : ite;
			}
		});
	}

	private static boolean trimStackTrace(Throwable th) {
		var isTrimmed = false;

		// trims stack trace to appropriate length
		while (th != null) {
			var st0 = th.getStackTrace();

			if (maxStackTraceLength < st0.length) {
				var st1 = new StackTraceElement[maxStackTraceLength];
				Array_.copy(st0, 0, st1, 0, maxStackTraceLength);
				th.setStackTrace(st1);

				isTrimmed = true;
			}

			th = th.getCause();
		}

		return isTrimmed;
	}

}
