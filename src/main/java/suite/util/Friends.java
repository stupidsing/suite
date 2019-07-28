package suite.util;

import suite.util.Rethrow.SourceEx;

public class Friends {

	public static <T> T rethrow(SourceEx<T, Exception> source) {
		return Rethrow.ex(source);
	}

}
