package suite.util;

import java.text.DateFormat;
import java.text.SimpleDateFormat;

public class FormatUtil {

	private static final String ymd = "yyyy-MM-dd";
	private static final String hms = "HH:mm:ss";

	// Dang, the date formats and decimal formats are not thread-safe!! Wrap
	// them and make the method calls synchronised.
	public static final DateFormat dateFmt = SynchronizeUtil.proxy(DateFormat.class, new SimpleDateFormat(ymd));
	public static final DateFormat timeFmt = SynchronizeUtil.proxy(DateFormat.class, new SimpleDateFormat(hms));
	public static final DateFormat dtFmt = SynchronizeUtil.proxy(DateFormat.class, new SimpleDateFormat(ymd + " " + hms));

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
