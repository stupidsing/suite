package suite.sample;

import java.lang.Thread.State;
import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

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
		List<Entry<String, int[]>> entries = Util.sort(record.entrySet(), (e0, e1) -> e1.getValue()[0] - e0.getValue()[0]);

		StringBuilder sb = new StringBuilder();
		sb.append("PROFILING RESULT\n");
		sb.append("TOTAL SAMPLES = " + count.get() + "\n\n");

		sb.append(entries.stream().map(entry -> {
			String name = entry.getKey();
			int count = entry.getValue()[0];
			return String.format("%d\t%s\n", count, name);
		}).collect(Collectors.joining()));

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

				for (StackTraceElement elem : threadInfo.getStackTrace()) {
					String m = elem.getClassName() + "." + elem.getMethodName();
					String l = elem.getFileName() + ":" + elem.getLineNumber();
					elements.add(m + " (" + l + ")");
				}

				for (String name : elements)
					record.computeIfAbsent(name, any -> new int[] { 0 })[0]++;
			}
	}

}
