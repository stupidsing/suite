package suite.concurrent;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import primal.MoreVerbs.Read;
import primal.Verbs.Sleep;
import primal.adt.BooMutable;
import primal.persistent.PerList;
import suite.concurrent.Concurrent.DeadlockException;
import suite.concurrent.Mutex.MutexLock;
import suite.streamlet.As;

public class MutexTest {

	private Mutex a = new Mutex();
	private Mutex b = new Mutex();

	public interface MutexTestRunnable {
		public void run() throws DeadlockException;
	}

	@Test
	public void testDeadlock() {
		var ra = lockInOrder(PerList.of(a, b));
		var rb = lockInOrder(PerList.of(b, a));
		assertTrue(isDeadlock(ra, rb));
	}

	@Test
	public void testNoDeadlock() {
		var ra = lockInOrder(PerList.of(a, b));
		var rb = lockInOrder(PerList.of(a, b));
		assertFalse(isDeadlock(ra, rb));
	}

	private MutexTestRunnable lockInOrder(PerList<Mutex> ms) {
		return new MutexTestRunnable() {
			public void run() {
				run(ms);
			}

			private void run(PerList<Mutex> ms) {
				if (!ms.isEmpty())
					try (var mla = new MutexLock(ms.head)) {
						Sleep.quietly(500);
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
