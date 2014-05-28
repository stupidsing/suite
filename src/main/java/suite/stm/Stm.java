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

	public interface TransactionManager<Tx extends Transaction> {
		public Tx createTransaction(Tx parent);

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

	public static <Tx extends Transaction> boolean doTransaction(TransactionManager<Tx> transactionManager, Sink<Tx> sink) {
		Tx transaction = transactionManager.createTransaction(null);

		try {
			sink.sink(transaction);
			transaction.commit();
			return true;
		} catch (Exception ex) {
			transaction.rollback();
		}

		return false;
	}

}
