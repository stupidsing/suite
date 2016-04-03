package suite.util;

import java.io.IOException;

public class Rethrow {

	public interface SourceEx<T, Ex extends Throwable> {
		public T source() throws Ex;
	}

	public static <T> T ex(SourceEx<T, Exception> source) {
		try {
			return source.source();
		} catch (Exception ex) {
			throw new RuntimeException(ex);
		}
	}

	public static <T> T ioException(SourceEx<T, IOException> source) {
		try {
			return source.source();
		} catch (IOException ex) {
			throw new RuntimeException(ex);
		}
	}

}
