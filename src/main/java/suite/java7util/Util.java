package suite.java7util;

import java.util.Calendar;

@Deprecated
public class Util {

	public static Class<?> clazz(Object object) {
		return object != null ? object.getClass() : null;
	}

	public static long newDate(int year, int month, int day) {
		return newDate(year, month, day, 0, 0, 0);
	}

	public static long newDate(int year, int month, int day, int hour, int minute, int second) {
		Calendar cal = Calendar.getInstance();
		cal.clear();
		cal.set(year, Calendar.JANUARY + month - 1, day, hour, minute, second);
		return cal.getTimeInMillis();
	}

}
