package org.util;

import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class FormatUtil {

	private static final String ymd = "yyyy-MM-dd";
	private static final String hms = "HH:mm:ss";

	public static final SyncDateFormat dateFmt = new SyncDateFormat(ymd);
	public static final SyncDateFormat timeFmt = new SyncDateFormat(hms);
	public static final SyncDateFormat dtFmt = new SyncDateFormat(ymd + " " + hms);

	// Dang, the date formats and decimal formats are not thread-safe!! Wrap
	// them and make the method calls synchronised.

	public static class SyncDateFormat {
		private DateFormat dateFormat;

		public SyncDateFormat(String fmt) {
			dateFormat = new SimpleDateFormat(fmt);
		}

		public SyncDateFormat(DateFormat dateFormat) {
			this.dateFormat = dateFormat;
		}

		public synchronized String format(Date date) {
			return dateFormat.format(date);
		}

		public synchronized Date parse(String source) throws ParseException {
			return dateFormat.parse(source);
		}
	}

	public static class SynchronizedDecimalFormat {
		private DecimalFormat decimalFormat;

		public SynchronizedDecimalFormat(String fmt) {
			decimalFormat = new DecimalFormat(fmt);
		}

		public SynchronizedDecimalFormat(DecimalFormat decimalFormat) {
			this.decimalFormat = decimalFormat;
		}

		public synchronized String format(long n) {
			return decimalFormat.format(n);
		}

		public synchronized String format(double n) {
			return decimalFormat.format(n);
		}

		public synchronized Number parse(String source) throws ParseException {
			return decimalFormat.parse(source);
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
