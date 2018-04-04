package suite.util;

import java.util.Collection;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import suite.os.LogUtil;

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

	public static Thread newThread(RunnableEx runnable) {
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

	public static void startJoin(Collection<Thread> threads) {
		for (Thread thread : threads)
			thread.start();

		for (Thread thread : threads)
			try {
				thread.join();
			} catch (InterruptedException ex) {
				LogUtil.error(ex);
			}
	}

	public static Thread startThread(RunnableEx runnable) {
		Thread thread = newThread_(runnable);
		thread.start();
		return thread;
	}

	private static ThreadPoolExecutor newExecutor(int corePoolSize, int maxPoolSize) {
		return new ThreadPoolExecutor(corePoolSize, maxPoolSize, 10, TimeUnit.SECONDS, new ArrayBlockingQueue<>(256));
	}

	private static Thread newThread_(RunnableEx runnable) {
		return new Thread() {
			public void run() {
				try {
					runnable.run();
				} catch (Exception ex) {
					LogUtil.error(ex);
				}
			}
		};
	}

}
