package suite.util;

import java.io.InputStream;
import java.io.Reader;
import java.util.concurrent.atomic.AtomicInteger;

public class Util {

	private static AtomicInteger counter = new AtomicInteger();

	public static void assert_(boolean b) {
		if (!b)
			throw new AssertionError();
	}

	/**
	 * Reads a line from a stream with a maximum line length limit. Removes
	 * carriage return if it is DOS-mode line feed (CR-LF). Unknown behaviour
	 * when dealing with non-ASCII encoding characters.
	 */
	public static String readLine(InputStream is) {
		return Rethrow.ex(() -> {
			StringBuilder sb = new StringBuilder();
			int c;
			while (0 <= (c = is.read()) && c != 10) {
				sb.append((char) c);
				if (65536 <= sb.length())
					Fail.t("line too long");
			}
			return 0 <= c ? strip(sb) : null;
		});
	}

	public static String readLine(Reader reader) {
		return Rethrow.ex(() -> {
			StringBuilder sb = new StringBuilder();
			int c;
			while (0 <= (c = reader.read()) && c != 10) {
				sb.append((char) c);
				if (65536 <= sb.length())
					Fail.t("line too long");
			}
			return 0 <= c ? strip(sb) : null;
		});
	}

	public static int temp() {
		return counter.getAndIncrement();
	}

	private static String strip(StringBuilder sb) {
		int length = sb.length();
		if (0 < length && sb.charAt(length - 1) == 13)
			sb.deleteCharAt(length - 1);
		return sb.toString();
	}

}
