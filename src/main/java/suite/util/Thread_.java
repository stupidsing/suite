package suite.util;

import static primal.statics.Rethrow.ex;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import primal.os.Log_;
import suite.streamlet.Puller;
import suite.streamlet.Read;

public class Thread_ {

	public static class Th extends Thread {
		private RunnableEx runnable;

		public Th(RunnableEx runnable) {
			this.runnable = runnable;
		}

		public void run() {
			try {
				runnable.run();
			} catch (Exception ex) {
				Log_.error(ex);
			}
		}

		public void join_() {
			ex(() -> {
				join();
				return this;
			});
		}
	}

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
		return new Th(runnable);
	}

	public static void sleepQuietly(long time) {
		if (0 < time)
			try {
				Thread.sleep(time);
			} catch (InterruptedException ex) {
				Log_.error(ex);
			}
	}

	public static void startJoin(RunnableEx... rs) {
		var threads1 = Read.from(rs).map(Thread_::startThread).collect();
		threads1.sink(Th::join_);
	}

	public static Void startJoin(Puller<Th> threads0) {
		var threads1 = threads0.toList();
		threads1.forEach(Th::start);
		threads1.forEach(Th::join_);
		return null;
	}

	public static Th startThread(RunnableEx runnable) {
		var thread = new Th(runnable);
		thread.start();
		return thread;
	}

	private static ThreadPoolExecutor newExecutor(int corePoolSize, int maxPoolSize) {
		return new ThreadPoolExecutor(corePoolSize, maxPoolSize, 10, TimeUnit.SECONDS, new ArrayBlockingQueue<>(256));
	}

}
