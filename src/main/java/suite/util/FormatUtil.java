package suite.util;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class FormatUtil {

	private static String ymd = "yyyy-MM-dd";
	private static String hms = "HH:mm:ss";

	// Dang, the date formats and decimal formats are not thread-safe!! Wrap
	// them and make the method calls synchronised.
	public static SyncDateFormat dateFormat = new SyncDateFormat(ymd);
	public static SyncDateFormat timeFormat = new SyncDateFormat(hms);
	public static SyncDateFormat dateTimeFormat = new SyncDateFormat(ymd + " " + hms);

	public static class SyncDateFormat {
		private DateFormat df;

		public SyncDateFormat(String fmt) {
			df = new SimpleDateFormat(fmt);
		}

		public synchronized String format(Date date) {
			return df.format(date);
		}

		public synchronized Date parse(String s) throws ParseException {
			return df.parse(s);
		}
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
		while (--pos >= 0)
			if (!Character.isWhitespace(s.charAt(pos)))
				break;
		return s.substring(0, pos + 1);
	}

}
