package suite.stm;

import suite.util.FunUtil.Sink;

public class Stm {

	public static enum TransactionStatus {
		ABORTED, ACTIVE, COMMITTED
	}

	public static class AbortException extends TransactionException {
		private static final long serialVersionUID = 1l;
	}

	public static class DeadlockException extends TransactionException {
		private static final long serialVersionUID = 1l;
	}

	public static class TransactionException extends Exception {
		private static final long serialVersionUID = 1l;
	}

	public interface TransactionManager {
		public Transaction createTransaction();

		public <T> Memory<T> createMemory(Class<T> clazz);
	}

	public interface Transaction {
		public void commit() throws TransactionException;

		public void rollback();
	}

	public interface Memory<T> {
		public T read(Transaction transaction) throws InterruptedException, TransactionException;

		public void write(Transaction transaction, T t) throws InterruptedException, TransactionException;
	}

	public static boolean doTransaction(TransactionManager transactionManager, Sink<Transaction> fun) {
		Transaction transaction = transactionManager.createTransaction();

		try {
			try {
				fun.sink(transaction);
				return true;
			} finally {
				transaction.commit();
			}
		} catch (Exception ex) {
			transaction.rollback();
		}

		return false;
	}

}
