package org.util;

import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class FormatUtil {

	// Dang, the date formats and decimal formats are not thread-safe!! Wrap
	// them and make the method calls synchronised.

	public static class SynchronizedDateFormat {
		private DateFormat dateFormat;

		public SynchronizedDateFormat(String fmt) {
			this.dateFormat = new SimpleDateFormat(fmt);
		}

		public SynchronizedDateFormat(DateFormat dateFormat) {
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
			this.decimalFormat = new DecimalFormat(fmt);
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

}
