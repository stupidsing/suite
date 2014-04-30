package suite.sample;

import java.lang.Thread.State;
import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicInteger;

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
		List<Entry<String, int[]>> entries = Util.sort(record.entrySet(), new Comparator<Entry<String, int[]>>() {
			public int compare(Entry<String, int[]> e0, Entry<String, int[]> e1) {
				return e1.getValue()[0] - e0.getValue()[0];
			}
		});

		StringBuilder sb = new StringBuilder();
		sb.append("PROFILING RESULT\n");
		sb.append("TOTAL SAMPLES = " + count.get() + "\n\n");

		for (Entry<String, int[]> entry : entries) {
			String name = entry.getKey();
			int count = entry.getValue()[0];
			sb.append(String.format("%d\t%s\n", count, name));
		}

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

				for (String name : elements) {
					int counter[] = record.get(name);
					if (counter == null)
						record.put(name, counter = new int[] { 0 });
					counter[0]++;
				}
			}
	}

}
