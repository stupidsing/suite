package suite.util;

public class Fail {

	public static class InterruptedRuntimeException extends RuntimeException {
		private static final long serialVersionUID = 1l;

		public InterruptedRuntimeException(InterruptedException ex) {
			super(ex);
		}
	}

	public static boolean b(String m) {
		return t(m, null) != null;
	}

	public static <T> T t(String m, Throwable th) {
		if (th instanceof InterruptedException)
			throw new InterruptedRuntimeException((InterruptedException) th);
		else if (th instanceof RuntimeException && m == null)
			throw (RuntimeException) th;
		else
			throw new RuntimeException(m, th);
	}

	public static boolean v(boolean b) {
		return b ? b : t(null, null);
	}

}
