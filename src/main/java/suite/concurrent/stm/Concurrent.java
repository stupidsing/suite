package suite.concurrent.stm;

public class Concurrent {

	public static class AbortException extends TransactionException {
		private static final long serialVersionUID = 1l;
	}

	public static class DeadlockException extends TransactionException {
		private static final long serialVersionUID = 1l;
	}

	public static class LostSnapshotException extends TransactionException {
		private static final long serialVersionUID = 1l;
	}

	public static class TransactionException extends RuntimeException {
		private static final long serialVersionUID = 1l;
	}

}
