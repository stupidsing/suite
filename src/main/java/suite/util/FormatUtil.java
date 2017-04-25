package suite.util;

import java.time.format.DateTimeFormatter;
import java.util.Locale;

public class FormatUtil {

	public static DateTimeFormatter dateFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd", Locale.ENGLISH);
	public static DateTimeFormatter timeFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss", Locale.ENGLISH);

	public static String leftTrim(String s) {
		int pos = 0;
		do
			if (!Character.isWhitespace(s.charAt(pos)))
				break;
		while (++pos < s.length());
		return s.substring(pos);
	}

	public static String rightTrim(String s) {
		int pos = s.length();
		while (0 <= --pos)
			if (!Character.isWhitespace(s.charAt(pos)))
				break;
		return s.substring(0, pos + 1);
	}

}
