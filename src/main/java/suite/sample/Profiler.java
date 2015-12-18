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

import suite.net.Service;
import suite.streamlet.As;
import suite.streamlet.Read;
import suite.util.Util;

public class Profiler implements Service {

	private static int timerDuration = 50;
	private static int stackTraceDepth = 256;

	private Timer timer;
	private AtomicInteger count = new AtomicInteger();
	private Map<String, Record> records = new HashMap<>();

	private static class Record {
		private int count;
		private int minLineNumber = Integer.MAX_VALUE;
	}

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
		sb.append(Read.from(records) //
				.sort((p0, p1) -> p1.t1.count - p0.t1.count) //
				.map((name, record) -> String.format("%d\t%s:%d\n", record.count, name, record.minLineNumber)) //
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

				// Save line numbers as it is important to trace lambdas and
				// anonymous classes
				for (StackTraceElement elem : threadInfo.getStackTrace()) {
					String fileName = elem.getFileName();
					String name = elem.getClassName() //
							+ "." + elem.getMethodName() //
							+ " " + (fileName != null ? fileName : "<unknown>");
					if (elements.add(name)) {
						Record record = records.computeIfAbsent(name, any -> new Record());
						record.count++;
						record.minLineNumber = Math.min(record.minLineNumber, elem.getLineNumber());
					}
				}
			}
	}

}
