package suite.concurrent.stm;

import java.util.concurrent.atomic.AtomicStampedReference;

import primal.fp.Funs.Fun;
import suite.concurrent.stm.Concurrent.AbortException;
import suite.concurrent.stm.Concurrent.LostSnapshotException;
import suite.concurrent.stm.Stm.Transaction;
import suite.concurrent.stm.Stm.TransactionStatus;

/**
 * Implements software transactional memory by compare-and-swap operations.
 *
 * @author ywsing
 */
public class ObstructionFreeStm {

	public class Memory<V> {

		// reference points to the latest snapshot; stamp is the last read time
		private AtomicStampedReference<Snapshot<V>> asr;

		public Memory(V value) {
			asr = new AtomicStampedReference<>(new Snapshot<>(ambient, value, null), 0);
		}

		private void trim() {
			var lastReadTime = new int[1];
			var snapshot = asr.get(lastReadTime);
			while (snapshot.owner.status == TransactionStatus.ROLLBACK)
				snapshot = snapshot.previous;

			var i = 0;
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
		var transaction = begin();
		var ok = false;

		try {
			var result = fun.apply(transaction);
			ok = true;
			return result;
		} finally {
			transaction.end(ok ? TransactionStatus.DONE____ : TransactionStatus.ROLLBACK);
		}
	}

	public Transaction begin() {
		return new Transaction();
	}

	public <V> Memory<V> newMemory(V value) {
		return new Memory<>(value);
	}

	public <V> V get(Transaction transaction, Memory<V> memory) {
		while (true) {
			var lastReadTime = new int[1];
			var snapshot = memory.asr.get(lastReadTime);
			var snapshot1 = snapshot;

			// read committed, repeatable read
			while (snapshot1 != null //
					&& snapshot1.owner != transaction //
					&& (snapshot1.owner.status != TransactionStatus.DONE____ || transaction.time <= snapshot1.owner.time))
				snapshot1 = snapshot1.previous;

			if (snapshot1 == null)
				throw new LostSnapshotException();

			if (lastReadTime[0] < transaction.time
					&& !memory.asr.compareAndSet(snapshot, snapshot, lastReadTime[0], transaction.time))
				continue;

			return snapshot1.value;
		}
	}

	public <V> void put(Transaction transaction, Memory<V> memory, V value) {
		while (true) {
			var lastReadTime = new int[1];
			var snapshot = memory.asr.get(lastReadTime);

			// serializable
			if (transaction.time < lastReadTime[0])
				throw new AbortException();

			while (snapshot.owner.status == TransactionStatus.ROLLBACK)
				snapshot = snapshot.previous;

			if (snapshot.owner != transaction && snapshot.owner.status == TransactionStatus.ACTIVE__) {
				snapshot.owner.mutex.lock();
				snapshot.owner.mutex.unlock();
				continue;
			}

			var snapshot1 = new Snapshot<>(transaction, value, snapshot);

			if (!memory.asr.compareAndSet(snapshot, snapshot1, lastReadTime[0], transaction.time))
				continue;

			memory.trim();
			return;
		}
	}

}
