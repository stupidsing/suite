package suite.java7util;

import java.util.Calendar;

public class Util {

	public static long createDate(int year, int month, int day) {
		return createDate(year, month, day, 0, 0, 0);
	}

	public static long createDate(int year, int month, int day, int hour, int minute, int second) {
		Calendar cal = Calendar.getInstance();
		cal.clear();
		cal.set(year, Calendar.JANUARY + month - 1, day, hour, minute, second);
		return cal.getTimeInMillis();
	}

}
