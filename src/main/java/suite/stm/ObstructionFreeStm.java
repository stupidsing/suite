package suite.stm;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import suite.stm.ObstructionFreeStm.ObstructionFreeTransaction;
import suite.stm.Stm.AbortException;
import suite.stm.Stm.DeadlockException;
import suite.stm.Stm.Memory;
import suite.stm.Stm.Transaction;
import suite.stm.Stm.TransactionException;
import suite.stm.Stm.TransactionManager;
import suite.stm.Stm.TransactionStatus;

/**
 * Implements software transactional memory by locking.
 *
 * @author ywsing
 */
public class ObstructionFreeStm implements TransactionManager<ObstructionFreeTransaction> {

	private static final int nullTimestamp = -1;

	private AtomicInteger clock = new AtomicInteger();

	@Override
	public ObstructionFreeTransaction createTransaction(ObstructionFreeTransaction parent) {
		ObstructionFreeTransaction transaction = new ObstructionFreeTransaction();
		transaction.parent = parent;
		return transaction;
	}

	@Override
	public <T> Memory<T> createMemory(Class<T> clazz) {
		return new ObstructionFreeMemory<>();
	}

	protected class ObstructionFreeTransaction implements Transaction {
		private volatile TransactionStatus status = TransactionStatus.ACTIVE;
		private volatile ObstructionFreeTransaction waitingFor;
		private volatile int readTimestamp = clock.getAndIncrement();
		private volatile int commitTimestamp = nullTimestamp;
		private Set<ObstructionFreeMemory<?>> readMemories = new HashSet<>();
		private ObstructionFreeTransaction parent;

		/**
		 * Tries to finish a transaction. Make sure all children committed
		 * before committing the parent.
		 *
		 * @throws AbortException
		 */
		public void commit() throws AbortException {
			boolean isCommit = true;
			commitTimestamp = clock.getAndIncrement();

			// If some touched values are discovered to be modified between our
			// read/write time, we must abort
			for (ObstructionFreeMemory<?> memory : readMemories)
				isCommit &= isDescendantOf(memory.owner.get(), this) //
						|| memory.timestamp0 <= readTimestamp //
						|| memory.timestamp0 >= commitTimestamp;

			if (isCommit)
				setStatus(TransactionStatus.COMMITTED);
			else
				throw new AbortException();
		}

		/**
		 * Abort a transaction. Must abort all children first.
		 */
		public void rollback() {
			setStatus(TransactionStatus.ABORTED);
		}

		private synchronized void setStatus(TransactionStatus status1) {
			status = status1;
			notifyAll();
		}

		private void waitForCompletion() throws InterruptedException {
			if (status == TransactionStatus.ACTIVE)
				synchronized (this) {
					while (status == TransactionStatus.ACTIVE)
						wait();
				}
		}
	}

	private class ObstructionFreeMemory<T> implements Memory<T> {
		private AtomicReference<ObstructionFreeTransaction> owner = new AtomicReference<>();
		private volatile int timestamp0;
		private volatile T value0;
		private volatile T value1;

		/**
		 * Read would obtain the value between two checks of the owner.
		 *
		 * If the owner or its status is found to be changed, read needs to be
		 * performed again.
		 *
		 * Perform timestamp checking to avoid reading too up-to-date data.
		 */
		public T read(Transaction transaction) throws AbortException {
			ObstructionFreeTransaction ourTransaction = (ObstructionFreeTransaction) transaction;

			while (true) {
				ObstructionFreeTransaction theirTransaction0 = owner.get();
				TransactionStatus theirStatus0 = theirTransaction0 != null ? theirTransaction0.status : null;
				long timestamp;
				T value;

				if (theirStatus0 != TransactionStatus.COMMITTED //
						&& !isDescendantOf(ourTransaction, theirTransaction0)) {
					timestamp = timestamp0;
					value = value0;
				} else {
					timestamp = theirTransaction0.commitTimestamp;
					value = value1;
				}

				ObstructionFreeTransaction theirTransaction1 = owner.get();
				TransactionStatus theirStatus1 = theirTransaction1 != null ? theirTransaction1.status : null;

				// Retries if owner or owner status changed
				if (theirTransaction0 == theirTransaction1 && theirStatus0 == theirStatus1)
					if (isDescendantOf(ourTransaction, theirTransaction0) || timestamp <= ourTransaction.readTimestamp) {
						ourTransaction.readMemories.add(this);
						return value;
					} else
						throw new AbortException();
			}
		}

		/**
		 * Write would obtain the owner right.
		 *
		 * If someone else is the owner, the write would block until that owner
		 * completes. Simple waiting hierarchy is implemented in transaction
		 * class to detect deadlocks.
		 *
		 * Timestamp checking is done to avoid changing post-modified data.
		 */
		public void write(Transaction transaction, T t) throws InterruptedException, TransactionException {
			ObstructionFreeTransaction ourTransaction = (ObstructionFreeTransaction) transaction;
			ObstructionFreeTransaction theirTransaction;

			// Makes ourself the owner
			while (!isDescendantOf(ourTransaction, theirTransaction = owner.get()))
				if (theirTransaction != null) {

					// Waits until previous owner complete
					ObstructionFreeStm.this.wait(ourTransaction, theirTransaction);

					if (owner.compareAndSet(theirTransaction, ourTransaction)) {
						if (theirTransaction.status == TransactionStatus.COMMITTED) {
							timestamp0 = theirTransaction.commitTimestamp;
							value0 = value1;
						}

						break;
					}
				} else if (owner.compareAndSet(theirTransaction, ourTransaction))
					break;

			if (timestamp0 <= ourTransaction.readTimestamp)
				value1 = t;
			else
				throw new AbortException();
		}
	}

	private void wait(ObstructionFreeTransaction waiter, ObstructionFreeTransaction waitee) throws InterruptedException,
			DeadlockException {
		synchronized (ObstructionFreeStm.class) {

			// Detect waiting cycles and abort if deadlock is happening
			if (!isWaitingFor(waitee, waiter))
				waiter.waitingFor = waitee;
			else {
				waiter.setStatus(TransactionStatus.ABORTED);
				throw new DeadlockException();
			}
		}

		try {
			waitee.waitForCompletion();
		} finally {
			waiter.waitingFor = null;
		}
	}

	private boolean isDescendantOf(ObstructionFreeTransaction descendant, Transaction ascendant) {
		if (descendant != null && ascendant != null)
			if (ascendant != this)
				return isDescendantOf(descendant.parent, ascendant);
			else
				return true;
		else
			return false;
	}

	private boolean isWaitingFor(ObstructionFreeTransaction waiter, ObstructionFreeTransaction waitee) {
		if (waiter != null && waitee != null)
			if (waiter != waitee)
				return isWaitingFor(waiter.waitingFor, waitee) || isWaitingFor(waiter, waitee.parent);
			else
				return true;
		else
			return false;
	}

}
