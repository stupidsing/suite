package suite.util;

import suite.util.Rethrow.SourceEx;

public class Friends {

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
		return Fail.t(m, th);
	}

	public static <T> T rethrow(SourceEx<T, Exception> source) {
		return Rethrow.ex(source);
	}

}
