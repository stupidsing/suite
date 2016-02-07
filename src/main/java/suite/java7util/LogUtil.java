package suite.java7util;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Proxy;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import suite.inspect.Dump;
import suite.util.Copy;

@Deprecated
public class LogUtil {

	private static int maxStackTraceLength = 99;

	public static <I> I proxy(Class<I> interface_, I object) {
		@SuppressWarnings("unchecked")
		Class<I> clazz = (Class<I>) object.getClass();
		Log log = LogFactory.getLog(clazz);

		InvocationHandler handler = (proxy, method, ps) -> {
			String methodName = method.getName();
			String prefix = methodName + "()\n";
			StringBuilder sb = new StringBuilder();

			sb.append(prefix);

			if (ps != null)
				for (int i = 0; i < ps.length; i++)
					Dump.object(sb, "p" + i, ps[i]);

			log.info(sb.toString());

			try {
				Object value = method.invoke(object, ps);
				String rd = Dump.object("return", value);
				log.info(prefix + rd);
				return value;
			} catch (InvocationTargetException ite) {
				Throwable th = ite.getTargetException();
				boolean isTrimmed = trimStackTrace(th);
				log.error(prefix + (isTrimmed ? "(Trimmed)" : ""), th);
				throw th instanceof Exception ? (Exception) th : ite;
			}
		};

		ClassLoader classLoader = clazz.getClassLoader();
		Class<?> classes[] = { interface_ };

		@SuppressWarnings("unchecked")
		I proxied = (I) Proxy.newProxyInstance(classLoader, classes, handler);
		return proxied;
	}

	private static boolean trimStackTrace(Throwable th) {
		boolean isTrimmed = false;

		// Trims stack trace to appropriate length
		while (th != null) {
			StackTraceElement st0[] = th.getStackTrace();

			if (maxStackTraceLength < st0.length) {
				StackTraceElement st1[] = new StackTraceElement[maxStackTraceLength];
				Copy.array(st0, 0, st1, 0, maxStackTraceLength);
				th.setStackTrace(st1);

				isTrimmed = true;
			}

			th = th.getCause();
		}

		return isTrimmed;
	}

}
