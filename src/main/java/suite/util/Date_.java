package suite.util;

import java.time.ZoneId;
import java.time.ZonedDateTime;

public class Date_ {

	public static long newDate(int year, int month, int day) {
		return newDate(year, month, day, 0, 0, 0);
	}

	public static long newDate(int year, int month, int day, int hour, int minute, int second) {
		return ZonedDateTime.of(year, month, day, hour, minute, second, 0, ZoneId.systemDefault()).toEpochSecond() * 1000l;
	}

}
