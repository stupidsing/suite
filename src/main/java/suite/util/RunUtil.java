package suite.util;

import org.apache.log4j.Level;

import suite.object.Object_;
import suite.os.LogUtil;
import suite.primitive.IntMutable;
import suite.primitive.IntPrimitives.IntSource;
import suite.sample.Profiler;

public class RunUtil {

	public enum RunOption {
		RUN____, PROFILE, TIME___,
	};

	public static abstract class ExecutableProgram implements AutoCloseable {
		protected abstract boolean run(String[] args) throws Exception;

		public void close() {
		}
	}

	public static boolean isUnix() {
		var os = System.getenv("OS");
		return os == null || !os.startsWith("Windows");
	}

	public static void run(Class<? extends ExecutableProgram> clazz, String[] args) {
		run(clazz, args, RunOption.RUN____);
	}

	public static void run(Class<? extends ExecutableProgram> clazz, String[] args, RunOption runOption) {
		LogUtil.initLog4j(Level.INFO);
		var mutableCode = IntMutable.nil();

		IntSource source = () -> {
			try {
				try (ExecutableProgram main_ = Object_.new_(clazz)) {
					return main_.run(args) ? 0 : 1;
				}
			} catch (Throwable ex) {
				ex.printStackTrace();
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

		var code = mutableCode.value();
		if (code != 0)
			System.exit(code);
	}

}
