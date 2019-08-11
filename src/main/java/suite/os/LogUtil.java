package suite.os;

import java.lang.reflect.InvocationTargetException;

import primal.Verbs.Build;
import primal.fp.Funs.Source;
import primal.os.Log_;
import suite.inspect.Dump;
import suite.jdk.proxy.Intercept;

public class LogUtil {

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
		Log_.info("Enter " + message);
		try {
			return source.g();
		} finally {
			Log_.info("Exit " + message);
		}
	}

	public static <I> I proxy(Class<I> interface_, I object) {
		var tag = "[" + object.getClass().getSimpleName() + "] ";

		return Intercept.object(interface_, object, invocation -> (m, ps) -> {
			var methodName = m.getName();
			var prefix = methodName + "()\n";

			var dump = Build.string(sb -> {
				sb.append(prefix);

				if (ps != null)
					for (var i = 0; i < ps.length; i++)
						Dump.toDetails("p" + i, ps[i], sb);
			});

			Log_.info(tag + dump);

			try {
				var value = invocation.invoke(m, ps);
				var rd = Dump.toDetails("return", value);
				Log_.info(tag + prefix + rd);
				return value;
			} catch (InvocationTargetException ite) {
				var th = ite.getTargetException();
				Log_.error(tag + prefix, th);
				throw th instanceof Exception ? (Exception) th : ite;
			}
		});
	}

	private static ThreadLocal<String> prefix = ThreadLocal.withInitial(() -> "");

}
