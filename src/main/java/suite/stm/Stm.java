package suite.stm;

public class Stm {

	public static enum TransactionStatus {
		ABORTED, ACTIVE, COMMITTED
	}

	public interface TransactionSink {
		public void sink(Transaction transaction) throws InterruptedException, TransactionException;
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

	public interface TransactionManager<Tx extends Transaction> {
		public Tx createTransaction(Tx parent);

		public <T> Memory<T> createMemory(Class<T> clazz, T value);
	}

	public interface Transaction {
		public void commit() throws TransactionException;

		public void rollback();
	}

	public interface Memory<T> {
		public T read(Transaction transaction) throws InterruptedException, TransactionException;

		public void write(Transaction transaction, T value) throws InterruptedException, TransactionException;
	}

	public static <Tx extends Transaction> boolean doTransaction(TransactionManager<Tx> transactionManager, TransactionSink sink)
			throws InterruptedException, TransactionException {
		Tx transaction = transactionManager.createTransaction(null);
		boolean isCommitted = false;

		try {
			sink.sink(transaction);
			transaction.commit();
			isCommitted = true;
		} finally {
			if (!isCommitted)
				transaction.rollback();
		}

		return isCommitted;
	}

}
