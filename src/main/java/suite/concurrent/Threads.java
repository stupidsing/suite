package suite.concurrent;

import static suite.util.Streamlet_.forInt;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;

import primal.Verbs.RunnableEx;
import primal.Verbs.Sleep;
import primal.Verbs.Start;
import primal.fp.Funs.Sink;
import primal.os.Log_;
import suite.concurrent.Condition.Cond;

public class Threads implements AutoCloseable {

	// if thread A is has a queue shorter than first threshold
	// and B has a queue longer than second threshold
	// then A can steal from B
	private static int stealThreshold0 = 0;
	private static int stealThreshold1 = 128;

	private static int BORED__ = 0;
	private static int ATWORK_ = 1;
	private static int TOOMUCH = 2;
	private static int STOPPED = 3;

	private Map<Thread, ThreadData> threadDataByThread = new HashMap<>();

	private volatile ThreadData boredThread; // thread with least workload, i.e. shortest queue

	private class ThreadData extends Condition {
		private volatile int state;
		private Deque<RunnableEx> queue = new ArrayDeque<>();

		private void doQueue(Sink<Deque<RunnableEx>> sink) {
			lock(() -> {
				sink.f(queue);
				updateState();
			});

			if (state == TOOMUCH && boredThread.state == BORED__) // shout anyway
				boredThread.doQueue(queue -> boredThread.notify());
			// for (var td : threadDataByThread.values()) td.doQueue(queue -> td.notify());
		}

		private void updateState() {
			var size = queue.size();
			int state_;
			if (size <= stealThreshold0)
				state_ = BORED__;
			else if (size < stealThreshold1)
				state_ = ATWORK_;
			else
				state_ = TOOMUCH;

			if (state != state_)
				state = state_;

			updateBored(this);
		}
	}

	public Threads(int nThreads) {
		forInt(nThreads).sink(i -> {
			var td = new ThreadData();
			var thread = Start.thread(() -> dispatchLoop(td));
			threadDataByThread.put(thread, td);
			boredThread = td;
		});
	}

	private void dispatchLoop(ThreadData td) {
		while (td.state != STOPPED) {
			var cond = new Cond() {
				private RunnableEx runnable;
				private ThreadData maxThread;

				public boolean ok() {
					var b0 = td.state == STOPPED;
					var b1 = !td.queue.isEmpty();

					if (b1) {
						runnable = td.queue.removeFirst();
						td.updateState();
					}

					if (td.state == BORED__) // hungry
						for (var td_ : threadDataByThread.values())
							if (td_.state == TOOMUCH)
								maxThread = td_;

					var b2 = maxThread != null;

					return b0 || b1 || b2;
				}
			};

			td.waitTill(cond);

			var runnable = cond.runnable;
			var frThread = cond.maxThread; // steal work from another

			if (runnable != null)
				try {
					runnable.run();
				} catch (Exception ex) {
					Log_.error(ex);
				}

			if (frThread != null && frThread.state == TOOMUCH) {
				td.satisfy(() -> {
					if (td.state == BORED__) {
						var list = new ArrayList<RunnableEx>();

						// steal jobs from the long queue of our thread
						frThread.doQueue(queue -> {
							for (var j = 0; j < 32; j++)
								if (!queue.isEmpty())
									list.add(queue.removeFirst());
						});

						// and put them in the thread with the shortest queue
						list.forEach(td.queue::addLast);
						td.updateState();
					}
				});

				// the shortest / longest queue thread would now belong to another
				threadDataByThread.values().forEach(this::updateBored);
			}
		}
	}

	public void execute(RunnableEx runnable) {
		var me = Thread.currentThread();
		var td0 = threadDataByThread.get(me);
		var td1 = td0 != null ? td0 : boredThread;
		td1.satisfy(() -> td1.doQueue(queue -> queue.addLast(runnable)));
	}

	@Override
	public void close() {
		for (var td : threadDataByThread.values())
			while (td.state != STOPPED) {
				td.doQueue(queue -> {
					if (queue.isEmpty()) {
						td.state = STOPPED;
						td.notify();
					}
				});
				Sleep.quietly(250l);
			}
	}

	private void updateBored(ThreadData td) {
		if (td.state == BORED__ && boredThread.state != BORED__)
			boredThread = td;
	}

}
