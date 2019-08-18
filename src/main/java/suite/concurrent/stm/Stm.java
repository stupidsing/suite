package suite.concurrent.stm;

import java.util.concurrent.atomic.AtomicInteger;

import suite.concurrent.Mutex;

public class Stm {

	private static AtomicInteger clock = new AtomicInteger();

	public enum TransactionStatus {
		ACTIVE__, DONE____, ROLLBACK,
	}

	public static class Transaction {
		public volatile int time = clock.incrementAndGet();
		public volatile TransactionStatus status = TransactionStatus.ACTIVE__;
		public Mutex mutex = new Mutex();

		public Transaction() {
			mutex.lock();
		}

		public void end(TransactionStatus s) {
			status = s;
			mutex.unlock();
		}
	}

}
