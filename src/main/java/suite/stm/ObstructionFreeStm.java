package suite.stm;

import java.util.concurrent.atomic.AtomicReference;

import suite.stm.Stm.DeadlockException;
import suite.stm.Stm.Memory;
import suite.stm.Stm.Transaction;
import suite.stm.Stm.TransactionException;
import suite.stm.Stm.TransactionManager;

/**
 * Implements software transactional memory by locking.
 * 
 * Less aborts (only on dead-locks), but prone to waits.
 * 
 * @author ywsing
 */
public class ObstructionFreeStm implements TransactionManager {

	private enum TransactionStatus {
		ABORTED, ACTIVE, COMMITTED
	}

	private static class ObstructionFreeTransaction implements Transaction {
		private volatile TransactionStatus status = TransactionStatus.ACTIVE;
		private ObstructionFreeTransaction waitingFor;

		public void commit() throws TransactionException {
			setStatus(TransactionStatus.COMMITTED);
		}

		public void rollback() {
			setStatus(TransactionStatus.ABORTED);
		}

		private void waitForAnotherTransaction(ObstructionFreeTransaction target) throws InterruptedException, TransactionException {
			synchronized (ObstructionFreeStm.class) {
				ObstructionFreeTransaction t = target;

				// Detects waiting cycles and abort if deadlock is happening
				while (t != null)
					if (t != this)
						t = t.waitingFor;
					else {
						setStatus(TransactionStatus.ABORTED);
						throw new DeadlockException();
					}

				waitingFor = target;
			}

			try {
				target.waitStatus();
			} finally {
				waitingFor = null;
			}
		}

		private void waitStatus() throws InterruptedException {
			if (status == TransactionStatus.ACTIVE)
				synchronized (this) {
					while (status == TransactionStatus.ACTIVE)
						wait();
				}
		}

		private synchronized void setStatus(TransactionStatus status1) {
			status = status1;
			notifyAll();
		}
	}

	private static class ObstructionFreeMemory<T> implements Memory<T> {
		private AtomicReference<ObstructionFreeTransaction> owner = new AtomicReference<>();
		private volatile T value0;
		private volatile T value1;

		public T read(Transaction transaction) {
			ObstructionFreeTransaction theirTransaction = owner.get();

			// Finishes other committed transactions
			if (theirTransaction != null //
					&& theirTransaction.status == TransactionStatus.COMMITTED //
					&& owner.compareAndSet(theirTransaction, null))
				value0 = value1;

			return value0;

		}

		public void write(Transaction transaction, T t) throws InterruptedException, TransactionException {
			ObstructionFreeTransaction ourTransaction = (ObstructionFreeTransaction) transaction;
			ObstructionFreeTransaction theirTransaction;

			// Makes ourself the owner
			while ((theirTransaction = owner.get()) != ourTransaction)
				if (theirTransaction != null) {

					// Waits until previous owner complete
					ourTransaction.waitForAnotherTransaction(theirTransaction);

					if (owner.compareAndSet(theirTransaction, ourTransaction)) {
						if (theirTransaction.status == TransactionStatus.COMMITTED)
							value0 = value1;

						break;
					}
				} else if (owner.compareAndSet(theirTransaction, ourTransaction))
					break;

			value1 = t;
		}
	}

	@Override
	public Transaction createTransaction() {
		return new ObstructionFreeTransaction();
	}

	@Override
	public <T> Memory<T> createMemory(Class<T> clazz) {
		return new ObstructionFreeMemory<>();
	}

}
