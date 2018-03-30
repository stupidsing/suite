package suite.util;

public class Fail {

	public static boolean b(String m) {
		return t(m, null) != null;
	}

	public static <T> T t() {
		return t(null, null);
	}

	public static <T> T t(String m) {
		return t(m, null);
	}

	public static <T> T t(Throwable th) {
		return t(null, th);
	}

	public static <T> T t(String m, Throwable th) {
		throw new RuntimeException(m, th);
	}

}
