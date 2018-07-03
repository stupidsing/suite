package suite.concurrent;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import suite.concurrent.Concurrent.DeadlockException;
import suite.concurrent.Mutex.MutexLock;
import suite.primitive.BooMutable;
import suite.streamlet.As;
import suite.streamlet.Read;
import suite.util.Thread_;

public class MutexTest {

	public interface MutexTestRunnable {
		public void run() throws DeadlockException;
	}

	@Test
	public void testDeadlock() {
		var a = new Mutex();
		var b = new Mutex();

		MutexTestRunnable ra = () -> {
			try (var mla = new MutexLock(a)) {
				Thread_.sleepQuietly(500);
				try (var mlb = new MutexLock(b)) {
				}
			}
		};
		MutexTestRunnable rb = () -> {
			try (var mlb = new MutexLock(b)) {
				Thread_.sleepQuietly(500);
				try (var mla = new MutexLock(a)) {
				}
			}
		};

		assertTrue(isDeadlock(ra, rb));
	}

	@Test
	public void testNoDeadlock() {
		var a = new Mutex();
		var b = new Mutex();

		MutexTestRunnable ra = () -> {
			try (var mla = new MutexLock(a)) {
				Thread_.sleepQuietly(500);
				try (var mlb = new MutexLock(b)) {
				}
			}
		};
		MutexTestRunnable rb = () -> {
			try (var mla = new MutexLock(a)) {
				Thread_.sleepQuietly(500);
				try (var mlb = new MutexLock(b)) {
				}
			}
		};

		assertFalse(isDeadlock(ra, rb));
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
