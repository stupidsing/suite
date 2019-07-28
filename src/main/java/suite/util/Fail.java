package suite.util;

public class Fail {

	public static class InterruptedRuntimeException extends RuntimeException {
		private static final long serialVersionUID = 1l;

		public InterruptedRuntimeException(InterruptedException ex) {
			super(ex);
		}
	}

	public static <T> T fail() {
		return fail(null, null);
	}

	public static <T> T fail(String m) {
		return fail(m, null);
	}

	public static <T> T fail(Throwable th) {
		return fail(null, th);
	}

	public static <T> T fail(String m, Throwable th) {
		return t(m, th);
	}

	public static boolean failBool(String m) {
		return t(m, null) != null;
	}

	private static <T> T t(String m, Throwable th) {
		if (th instanceof InterruptedException)
			throw new InterruptedRuntimeException((InterruptedException) th);
		else if (th instanceof RuntimeException && m == null)
			throw (RuntimeException) th;
		else
			throw new RuntimeException(m, th);
	}

}
