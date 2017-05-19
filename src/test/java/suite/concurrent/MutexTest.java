package suite.concurrent;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.junit.Test;

import suite.concurrent.Concurrent.DeadlockException;
import suite.concurrent.Mutex.MutexLock;
import suite.streamlet.Read;
import suite.util.IntMutable;
import suite.util.Thread_;

public class MutexTest {

	public interface MutexTestRunnable {
		public void run() throws DeadlockException;
	}

	@Test
	public void testDeadlock() throws InterruptedException {
		Mutex a = new Mutex();
		Mutex b = new Mutex();

		MutexTestRunnable ra = () -> {
			try (MutexLock mla = new MutexLock(a)) {
				Thread_.sleepQuietly(500);
				try (MutexLock mlb = new MutexLock(b)) {
				}
			}
		};
		MutexTestRunnable rb = () -> {
			try (MutexLock mlb = new MutexLock(b)) {
				Thread_.sleepQuietly(500);
				try (MutexLock mla = new MutexLock(a)) {
				}
			}
		};

		assertTrue(isDeadlock(ra, rb));
	}

	@Test
	public void testNoDeadlock() throws InterruptedException {
		Mutex a = new Mutex();
		Mutex b = new Mutex();

		MutexTestRunnable ra = () -> {
			try (MutexLock mla = new MutexLock(a)) {
				Thread_.sleepQuietly(500);
				try (MutexLock mlb = new MutexLock(b)) {
				}
			}
		};
		MutexTestRunnable rb = () -> {
			try (MutexLock mla = new MutexLock(a)) {
				Thread_.sleepQuietly(500);
				try (MutexLock mlb = new MutexLock(b)) {
				}
			}
		};

		assertFalse(isDeadlock(ra, rb));
	}

	private boolean isDeadlock(MutexTestRunnable... mtrs) throws InterruptedException {
		IntMutable result = IntMutable.false_();
		List<Thread> threads = Read.from(mtrs) //
				.map(mtr -> Thread_.newThread(() -> {
					try {
						mtr.run();
					} catch (DeadlockException ex1) {
						result.setTrue();
					}
				})) //
				.toList();
		Thread_.startJoin(threads);
		return result.isTrue();
	}

}
