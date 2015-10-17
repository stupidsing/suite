package suite.concurrent;

import java.util.concurrent.atomic.AtomicStampedReference;

import suite.concurrent.Stm.Transaction;
import suite.concurrent.Stm.TransactionStatus;
import suite.util.FunUtil.Fun;

/**
 * Implements software transactional memory by locking.
 *
 * @author ywsing
 */
public class ObstructionFreeStm {

	public class Memory<V> {

		// Reference is the most recent snapshot; stamp is the last read time
		private AtomicStampedReference<Snapshot<V>> asr;

		public Memory(V value) {
			asr = new AtomicStampedReference<>(new Snapshot<>(ambient, value, null), 0);
		}

		private void trim() {
			int lastReadTime[] = new int[1];
			Snapshot<V> snapshot = asr.get(lastReadTime);
			while (snapshot.owner.status == TransactionStatus.ROLLBACK)
				snapshot = snapshot.previous;

			int i = 0;
			while (snapshot != null && ++i < nSnapshots)
				snapshot = snapshot.previous;
			if (snapshot != null)
				snapshot.previous = null;
		}
	}

	private class Snapshot<V> {
		private Transaction owner;
		private V value;
		private Snapshot<V> previous;

		private Snapshot(Transaction owner, V value, Snapshot<V> previous) {
			this.owner = owner;
			this.value = value;
			this.previous = previous;
		}
	}

	private int nSnapshots = 3;
	private Transaction ambient;

	public ObstructionFreeStm() {
		ambient = new Transaction();
		ambient.status = TransactionStatus.DONE____;
	}

	public <T> T transaction(Fun<Transaction, T> fun) {
		Transaction transaction = begin();
		boolean ok = false;

		try {
			T result = fun.apply(transaction);
			ok = true;
			return result;
		} finally {
			transaction.end(ok ? TransactionStatus.DONE____ : TransactionStatus.ROLLBACK);
		}
	}

	public Transaction begin() {
		return new Transaction();
	}

	public <V> Memory<V> create(V value) {
		return new Memory<>(value);
	}

	public <V> V get(Transaction transaction, Memory<V> memory) {
		while (true) {
			int lastReadTime[] = new int[1];
			Snapshot<V> snapshot = memory.asr.get(lastReadTime);

			// Read committed, repeatable read
			while (snapshot != null
					&& !(snapshot.owner.status == TransactionStatus.DONE____ && snapshot.owner.time < transaction.time))
				snapshot = snapshot.previous;

			if (snapshot == null)
				throw new RuntimeException("Snapshot lost");

			if (!memory.asr.compareAndSet(snapshot, snapshot, lastReadTime[0], Math.max(lastReadTime[0], transaction.time)))
				continue;

			return snapshot.value;
		}
	}

	public <V> void put(Transaction transaction, Memory<V> memory, V value) {
		while (true) {
			int lastReadTime[] = new int[1];
			Snapshot<V> snapshot = memory.asr.get(lastReadTime);

			// Serializable
			if (transaction.time < lastReadTime[0])
				throw new RuntimeException("Abort");

			while (snapshot.owner.status == TransactionStatus.ROLLBACK)
				snapshot = snapshot.previous;

			if (snapshot.owner.status == TransactionStatus.ACTIVE__) {
				snapshot.owner.mutex.lock();
				snapshot.owner.mutex.unlock();
				continue;
			}

			Snapshot<V> snapshot1 = new Snapshot<>(transaction, value, snapshot);
			if (!memory.asr.compareAndSet(snapshot, snapshot1, lastReadTime[0], transaction.time))
				continue;

			memory.trim();
			return;
		}
	}

}
