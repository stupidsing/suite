package suite.util;

import java.util.concurrent.Callable;

import org.apache.log4j.Level;

import primal.MoreVerbs.Read;
import primal.os.Log_;
import suite.os.Execute;
import suite.os.LogUtil;

public class RunUtil {

	public static void main(String[] args0) {
		var args = new String[] { "suite.ServerMain", };
		RunUtil.run(() -> {
			var streamlet = Read.from(args);
			var clazz = Class.forName(streamlet.first());
			return (Boolean) clazz.getMethod("run").invoke(clazz.getConstructor().newInstance());
		});
	}

	public static String get(String key, String defaultValue) {
		return get_(key, defaultValue);
	}

	public static boolean isLinux() {
		var os = System.getenv("OS");
		return os == null || !os.startsWith("Windows");
	}

	public static boolean isLinux64() {
		if (get_("USE_32BIT", null) != null)
			return false;
		else if (get_("USE_64BIT", null) != null)
			return true;
		else
			return RunUtil.isLinux() && Execute.shell("uname -a").contains("x86_64");
	}

	public static void run(Callable<Boolean> callable) {
		LogUtil.initLogging(Level.INFO);
		int code;

		try {
			code = callable.call() ? 0 : 1;
		} catch (Throwable ex) {
			ex.printStackTrace();
			Log_.fatal(ex);
			code = 2;
		}

		if (code != 0)
			System.exit(code);
	}

	private static String get_(String key, String defaultValue) {
		String value = null;
		value = value != null ? value : System.getenv(key);
		value = value != null ? value : System.getProperty(key);
		return value != null ? value : defaultValue;
	}

}
