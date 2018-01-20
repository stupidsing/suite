package suite.util;

public class Fail {

	public static int i() {
		return i(null);
	}

	public static int i(String m) {
		throw new RuntimeException(m);
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

	public static void v() {
		t(null, null);
	}

	public static void v(String m) {
		t(m, null);
	}

	public static void v(Throwable th) {
		t(null, th);
	}

	public static void v(String m, Throwable th) {
		throw new RuntimeException(m, th);
	}

}
