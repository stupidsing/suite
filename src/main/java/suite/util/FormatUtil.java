package suite.util;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAccessor;
import java.util.Locale;

public class FormatUtil {

	private static DateTimeFormatter dateFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd", Locale.ENGLISH);
	private static DateTimeFormatter dateTimeFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss", Locale.ENGLISH);

	public static LocalDate date(String s) {
		return LocalDate.parse(s, dateFormat);
	}

	public static LocalDateTime dateTime(String s) {
		return LocalDateTime.parse(s, dateTimeFormat);
	}

	public static String formatDate(TemporalAccessor date) {
		return dateFormat.format(date);
	}

	public static String formatDateTime(TemporalAccessor dateTime) {
		return dateTimeFormat.format(dateTime);
	}

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
