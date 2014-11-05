package suite.sample;

import java.lang.Thread.State;
import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicInteger;

import suite.streamlet.As;
import suite.streamlet.Read;
import suite.util.Util;

public class Profiler {

	private static int timerDuration = 50;
	private static int stackTraceDepth = 256;

	private Timer timer;
	private AtomicInteger count = new AtomicInteger();
	private Map<String, int[]> record = new HashMap<>();

	public String profile(Runnable runnable) {
		try {
			start();
			runnable.run();
		} finally {
			stop();
		}
		return dump();
	}

	public void start() {
		timer = new Timer();
		timer.schedule(new TimerTask() {
			public void run() {
				recordStats();
			}
		}, 0, timerDuration);
	}

	public void stop() {
		timer.cancel();
		timer.purge();
	}

	public String dump() {
		StringBuilder sb = new StringBuilder();
		sb.append("PROFILING RESULT\n");
		sb.append("TOTAL SAMPLES = " + count.get() + "\n\n");
		sb.append(Read.from(record) //
				.sort((p0, p1) -> p1.t1[0] - p0.t1[0]) //
				.map(pair -> {
					String name = pair.t0;
					int count = pair.t1[0];
					return String.format("%d\t%s\n", count, name);
				}) //
				.collect(As.joined("")));
		return sb.toString();
	}

	private void recordStats() {
		long currentThreadId = Thread.currentThread().getId();
		ThreadMXBean mx = ManagementFactory.getThreadMXBean();

		long threadIds[] = mx.getAllThreadIds();
		ThreadInfo threadInfos[] = mx.getThreadInfo(threadIds, stackTraceDepth);
		count.getAndIncrement();

		for (ThreadInfo threadInfo : threadInfos)
			if (threadInfo != null //
					&& threadInfo.getThreadId() != currentThreadId //
					&& threadInfo.getThreadState() == State.RUNNABLE //
					&& !Util.stringEquals(threadInfo.getThreadName(), "ReaderThread")) {
				Set<String> elements = new HashSet<>();

				for (StackTraceElement elem : threadInfo.getStackTrace())
					elements.add(elem.getClassName() + "." + elem.getMethodName() + " (" + elem.getFileName() + ")");

				for (String name : elements)
					record.computeIfAbsent(name, any -> new int[] { 0 })[0]++;
			}
	}

}
