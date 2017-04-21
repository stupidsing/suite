package suite.os;

import java.io.PrintWriter;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Path;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.DailyRollingFileAppender;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;

import suite.inspect.Dump;
import suite.os.TimeUtil.TimedResult;
import suite.proxy.Intercept;
import suite.util.Copy;
import suite.util.FunUtil.Source;
import suite.util.TempDir;

public class LogUtil {

	private static int maxStackTraceLength = 99;
	private static Log suiteLog = LogFactory.getLog("suite");

	static {
		initLog4j(Level.INFO);
	}

	public static void initLog4j(Level level) {
		Path logDir = TempDir.resolve("logs");

		PatternLayout layout = new PatternLayout("%d %-5p [%c{1}] %m%n");

		ConsoleAppender console = new ConsoleAppender(layout);
		console.setWriter(new PrintWriter(System.err));
		console.activateOptions();

		DailyRollingFileAppender file = new DailyRollingFileAppender();
		file.setFile(logDir.resolve("suite.log").toString());
		file.setDatePattern("'.'yyyyMMdd");
		file.setLayout(layout);
		file.activateOptions();

		Logger logger = Logger.getRootLogger();
		logger.setLevel(level);
		logger.removeAllAppenders();
		logger.addAppender(console);
		logger.addAppender(file);
	}

	public static <T> T duration(String m, Source<T> source) {
		TimedResult<T> tr = new TimeUtil().time(source);
		LogUtil.info(m + " in " + tr.duration + "ms, GC occurred " + tr.nGcs + " times in " + tr.gcDuration + " ms");
		return tr.result;
	}

	public static void info(String message) {
		suiteLog.info(message);
	}

	public static void warn(String message) {
		suiteLog.warn(message);
	}

	public static void error(String message) {
		suiteLog.error(message);
	}

	public static void error(Throwable th) {
		boolean isTrimmed = trimStackTrace(th);
		suiteLog.error(isTrimmed ? "(Trimmed)" : "", th);
	}

	public static void fatal(String message) {
		suiteLog.fatal(message);
	}

	public static void fatal(Throwable th) {
		boolean isTrimmed = trimStackTrace(th);
		suiteLog.fatal(isTrimmed ? "(Trimmed)" : "", th);
	}

	public static <T> T log(String message, Source<T> source) {
		info("Enter " + message);
		try {
			return source.source();
		} finally {
			info("Exit " + message);
		}
	}

	public static <I> I proxy(Class<I> interface_, I object) {
		Log log = LogFactory.getLog(object.getClass());

		return Intercept.object(interface_, object, invocation -> (m, ps) -> {
			String methodName = m.getName();
			String prefix = methodName + "()\n";
			StringBuilder sb = new StringBuilder();

			sb.append(prefix);

			if (ps != null)
				for (int i = 0; i < ps.length; i++)
					Dump.object(sb, "p" + i, ps[i]);

			log.info(sb.toString());

			try {
				Object value = invocation.invoke(m, ps);
				String rd = Dump.object("return", value);
				log.info(prefix + rd);
				return value;
			} catch (InvocationTargetException ite) {
				Throwable th = ite.getTargetException();
				boolean isTrimmed = trimStackTrace(th);
				log.error(prefix + (isTrimmed ? "(Trimmed)" : ""), th);
				throw th instanceof Exception ? (Exception) th : ite;
			}
		});
	}

	private static boolean trimStackTrace(Throwable th) {
		boolean isTrimmed = false;

		// trims stack trace to appropriate length
		while (th != null) {
			StackTraceElement[] st0 = th.getStackTrace();

			if (maxStackTraceLength < st0.length) {
				StackTraceElement[] st1 = new StackTraceElement[maxStackTraceLength];
				Copy.array(st0, 0, st1, 0, maxStackTraceLength);
				th.setStackTrace(st1);

				isTrimmed = true;
			}

			th = th.getCause();
		}

		return isTrimmed;
	}

}
