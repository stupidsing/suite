package suite.concurrent;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import suite.concurrent.Concurrent.DeadlockException;
import suite.concurrent.Mutex.MutexLock;
import suite.immutable.IList;
import suite.primitive.BooMutable;
import suite.streamlet.As;
import suite.streamlet.Read;
import suite.util.Thread_;

public class MutexTest {

	private Mutex a = new Mutex();
	private Mutex b = new Mutex();

	public interface MutexTestRunnable {
		public void run() throws DeadlockException;
	}

	@Test
	public void testDeadlock() {
		var ra = lockInOrder(IList.of(a, b));
		var rb = lockInOrder(IList.of(b, a));
		assertTrue(isDeadlock(ra, rb));
	}

	@Test
	public void testNoDeadlock() {
		var ra = lockInOrder(IList.of(a, b));
		var rb = lockInOrder(IList.of(a, b));
		assertFalse(isDeadlock(ra, rb));
	}

	private MutexTestRunnable lockInOrder(IList<Mutex> ms) {
		return new MutexTestRunnable() {
			public void run() {
				run(ms);
			}

			private void run(IList<Mutex> ms) {
				if (!ms.isEmpty())
					try (var mla = new MutexLock(ms.head)) {
						Thread_.sleepQuietly(500);
						run(ms.tail);
					}
			}
		};
	}

	private boolean isDeadlock(MutexTestRunnable... mtrs) {
		var result = BooMutable.false_();

		Read.from(mtrs).collect(As.executeThreads(mtr -> {
			try {
				mtr.run();
			} catch (DeadlockException ex1) {
				result.setTrue();
			}
		}));

		return result.isTrue();
	}

}
