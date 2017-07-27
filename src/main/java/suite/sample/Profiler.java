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
import suite.util.String_;

public class Profiler implements Service {

	private static int timerDuration = 50;
	private static int stackTraceDepth = 256;

	private Timer timer;
	private AtomicInteger count = new AtomicInteger();
	private Map<String, Record> records = new HashMap<>();
	private Call callRoot = new Call();

	private static class Record {
		private int count;
	}

	private static class Call {
		private int count;
		private Map<String, Call> callees = new HashMap<>();
	}

	public void profile(Runnable runnable) {
		try {
			start();
			runnable.run();
		} finally {
			stop();
		}
		System.out.println(dump());
	}

	public void start() {
		(timer = new Timer()).schedule(new TimerTask() {
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

		sb.append("PROFILING RESULT\n\n");
		sb.append("TOTAL SAMPLES = " + count.get() + "\n");
		sb.append("\n\n");

		sb.append("METHODS\n\n");
		sb.append(Read.from2(records) //
				.sort((p0, p1) -> p1.t1.count - p0.t1.count) //
				.map((name, record) -> String.format("%d\t%s\n", record.count, name)) //
				.collect(As.joined()));
		sb.append("\n\n");

		sb.append("CALLS\n\n");
		dumpCalls(sb, "", callRoot);

		return sb.toString();
	}

	private void dumpCalls(StringBuilder sb, String indent, Call call) {
		if (call != null)
			Read.from2(call.callees) //
					.sort((e0, e1) -> -Integer.compare(e0.t1.count, e1.t1.count)) //
					.sink((callee, call1) -> {
						sb.append(String.format("%d\t%s%s\n", call1.count, indent, callee));
						dumpCalls(sb, indent + "| ", call1);
					});
	}

	private void recordStats() {
		long currentThreadId = Thread.currentThread().getId();
		ThreadMXBean mx = ManagementFactory.getThreadMXBean();

		long[] threadIds = mx.getAllThreadIds();
		ThreadInfo[] threadInfos = mx.getThreadInfo(threadIds, stackTraceDepth);
		count.getAndIncrement();

		for (ThreadInfo threadInfo : threadInfos)
			if (threadInfo != null //
					&& threadInfo.getThreadId() != currentThreadId //
					&& threadInfo.getThreadState() == State.RUNNABLE //
					&& !String_.equals(threadInfo.getThreadName(), "ReaderThread")) {
				StackTraceElement[] stackTrace = threadInfo.getStackTrace();
				Set<String> elements = new HashSet<>();
				int i = stackTrace.length;
				Call call = callRoot;

				// save line numbers as it is important to trace lambdas and
				// anonymous classes
				while (0 < i) {
					StackTraceElement elem = stackTrace[--i];
					String fileName = elem.getFileName();
					int lineNumber = elem.getLineNumber();
					String mn = elem.getClassName() + "." + elem.getMethodName();
					String fn = fileName != null ? " " + fileName + (1 < lineNumber ? ":" + lineNumber : "") : "<unknown>";
					String name = mn + fn;

					(call = call.callees.computeIfAbsent(mn, any -> new Call())).count++;
					if (elements.add(name))
						records.computeIfAbsent(name, any -> new Record()).count++;
				}
			}
	}

}
