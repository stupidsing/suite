package org.util;

import java.io.PrintWriter;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

import org.apache.commons.logging.LogFactory;
import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.DailyRollingFileAppender;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;

import sun.reflect.Reflection;

public class LogUtil {

	private static boolean initialized = false;
	private static int maxStackTraceLength = 99;

	public static void initLog4j() {
		if (!initialized)
			initLog4j(Level.INFO);
	}

	public static void initLog4j(Level level) {
		String caller = Reflection.getCallerClass(2).getSimpleName();

		PatternLayout layout = new PatternLayout("%d %-5p [%c{1}] %m%n");

		ConsoleAppender console = new ConsoleAppender(layout);
		console.setWriter(new PrintWriter(System.out));
		console.activateOptions();

		DailyRollingFileAppender file = new DailyRollingFileAppender();
		file.setFile("logs/" + caller + ".log");
		file.setDatePattern("'.'yyyyMMdd");
		file.setLayout(layout);
		file.activateOptions();

		Logger logger = Logger.getRootLogger();
		logger.setLevel(level);
		logger.removeAllAppenders();
		logger.addAppender(console);
		logger.addAppender(file);

		initialized = true;
	}

	public static void info(String cat, String message) {
		initLog4j();
		LogFactory.getLog(cat).info(message);
	}

	public static void error(Class<?> clazz, Throwable th) {
		error(clazz.getName(), th);
	}

	public static void error(String cat, Throwable th) {
		initLog4j();
		boolean isTrimmed = trimStackTrace(th);
		LogFactory.getLog(cat).error(isTrimmed ? "(Trimmed)" : "", th);
	}

	private static boolean trimStackTrace(Throwable th) {
		boolean isTrimmed = false;

		// Trims stack trace to appropriate length
		while (th != null) {
			StackTraceElement st0[] = th.getStackTrace();

			if (st0.length > maxStackTraceLength) {
				StackTraceElement st1[] = new StackTraceElement[maxStackTraceLength];
				Util.copyArray(st0, 0, st1, 0, maxStackTraceLength);
				th.setStackTrace(st1);

				isTrimmed = true;
			}

			th = th.getCause();
		}

		return isTrimmed;
	}

	public static <I> I proxy(Class<I> interface_, final I object) {
		@SuppressWarnings("unchecked")
		final Class<I> clazz = (Class<I>) object.getClass();
		ClassLoader classLoader = clazz.getClassLoader();
		Class<?> classes[] = { interface_ };

		InvocationHandler handler = new InvocationHandler() {
			public Object invoke(Object proxy, Method method, Object ps[])
					throws Exception {
				String prefix = clazz.getSimpleName() //
						+ "." + method.getName() + "()\n";

				String pd = "";
				if (ps != null)
					for (int i = 0; i < ps.length; i++)
						pd += DumpUtil.dump("p" + i, ps[i]);

				info("proxy", prefix + pd);

				try {
					Object value = method.invoke(object, ps);
					String rd = DumpUtil.dump("return", value);
					info("proxy", prefix + rd);
					return value;
				} catch (Exception ex) {
					error("proxy", ex);
					throw ex;
				}
			}
		};

		@SuppressWarnings("unchecked")
		I proxied = (I) Proxy.newProxyInstance(classLoader, classes, handler);
		return proxied;
	}

}
