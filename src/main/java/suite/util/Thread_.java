package suite.util;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import suite.os.LogUtil;
import suite.streamlet.Streamlet;

public class Thread_ {

	public interface RunnableEx {
		public void run() throws Exception;
	}

	public static Class<?> currentClass() {
		try {
			return Class.forName(getStackTrace(3).getClassName());
		} catch (ClassNotFoundException ex) {
			ex.printStackTrace();
			return null;
		}
	}

	public static String currentMethod() {
		return getStackTrace(3).getMethodName();
	}

	public static String currentPackage() {
		var cls = getStackTrace(3).getClassName();
		var pos = cls.lastIndexOf(".");
		return cls.substring(0, pos);
	}

	public static StackTraceElement getStackTrace(int n) {
		return Thread.currentThread().getStackTrace()[n];
	}

	public static ThreadPoolExecutor newExecutor() {
		return newExecutor(8, 32);
	}

	public static ThreadPoolExecutor newExecutorByProcessors() {
		var nProcessors = Runtime.getRuntime().availableProcessors();
		return newExecutor(nProcessors, nProcessors);
	}

	public static Th newThread(RunnableEx runnable) {
		return newThread_(runnable);
	}

	public static void sleepQuietly(long time) {
		if (0 < time)
			try {
				Thread.sleep(time);
			} catch (InterruptedException ex) {
				LogUtil.error(ex);
			}
	}

	public static void startJoin(Streamlet<Th> threads) {
		threads.sink(Th::start);
		threads.sink(Th::join_);
	}

	public static Th startThread(RunnableEx runnable) {
		var thread = newThread_(runnable);
		thread.start();
		return thread;
	}

	private static ThreadPoolExecutor newExecutor(int corePoolSize, int maxPoolSize) {
		return new ThreadPoolExecutor(corePoolSize, maxPoolSize, 10, TimeUnit.SECONDS, new ArrayBlockingQueue<>(256));
	}

	private static Th newThread_(RunnableEx runnable) {
		return new Th(runnable);
	}

}
