package suite.sample;

import java.lang.Thread.State;
import java.lang.management.ManagementFactory;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicInteger;

import suite.streamlet.Read;
import suite.util.String_;

public class Profiler {

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
		return String_.build(sb -> {
			sb.append("PROFILING RESULT\n\n");
			sb.append("TOTAL SAMPLES = " + count.get() + "\n");
			sb.append("\n\n");

			sb.append("METHODS\n\n");
			sb.append(Read //
					.from2(records) //
					.sort((p0, p1) -> p1.v.count - p0.v.count) //
					.map((name, record) -> String.format("%d\t%s", record.count, name)) //
					.toLines());
			sb.append("\n\n");

			sb.append("CALLS\n\n");
			dumpCalls(sb, "", callRoot);
		});
	}

	private void dumpCalls(StringBuilder sb, String indent, Call call) {
		if (call != null)
			Read //
					.from2(call.callees) //
					.sort((e0, e1) -> -Integer.compare(e0.v.count, e1.v.count)) //
					.sink((callee, call1) -> {
						sb.append(String.format("%d\t%s%s\n", call1.count, indent, callee));
						dumpCalls(sb, indent + "| ", call1);
					});
	}

	private void recordStats() {
		var currentThreadId = Thread.currentThread().getId();
		var mx = ManagementFactory.getThreadMXBean();

		var threadIds = mx.getAllThreadIds();
		var threadInfos = mx.getThreadInfo(threadIds, stackTraceDepth);
		count.getAndIncrement();

		for (var threadInfo : threadInfos)
			if (threadInfo != null //
					&& threadInfo.getThreadId() != currentThreadId //
					&& threadInfo.getThreadState() == State.RUNNABLE //
					&& !String_.equals(threadInfo.getThreadName(), "ReaderThread")) {
				var stackTrace = threadInfo.getStackTrace();
				var elements = new HashSet<>();
				var i = stackTrace.length;
				var call = callRoot;

				// save line numbers as it is important to trace lambdas and
				// anonymous classes
				while (0 < i) {
					var elem = stackTrace[--i];
					var fileName = elem.getFileName();
					var lineNumber = elem.getLineNumber();
					var mn = elem.getClassName() + "." + elem.getMethodName();
					var fn = fileName != null ? " " + fileName + (1 < lineNumber ? ":" + lineNumber : "") : "<unknown>";
					var name = mn + fn;

					(call = call.callees.computeIfAbsent(mn, any -> new Call())).count++;
					if (elements.add(name))
						records.computeIfAbsent(name, any -> new Record()).count++;
				}
			}
	}

}
