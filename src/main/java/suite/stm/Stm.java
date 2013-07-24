package suite.stm;

public class Stm {

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

}
