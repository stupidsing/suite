package suite.trade;

import static suite.util.Friends.fail;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Month;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Objects;

import suite.object.Object_;
import suite.util.To;

/**
 * Wraps LocalDateTime with a shorter class name.
 *
 * @author ywsing
 */
public class Time implements Comparable<Time> {

	public static Time MIN = of(LocalDateTime.MIN);
	public static Time MAX = of(LocalDateTime.MAX);
	private static int timeZone = 8;

	private LocalDateTime dateTime;

	public static int compare(Time time0, Time time1) {
		var ldt0 = time0.dateTime;
		var ldt1 = time1.dateTime;
		return Long.compare(ldt0.toEpochSecond(ZoneOffset.UTC), ldt1.toEpochSecond(ZoneOffset.UTC));
	}

	public static Time today() {
		return of(LocalDate.now().atStartOfDay());
	}

	public static Time now() {
		return of(LocalDateTime.now());
	}

	public static Time of(String s) {
		if (s.contains("-"))
			if (s.length() == 10)
				return ofYmd(s);
			else
				return ofYmdHms(s);
		else
			return fail("invalid time " + s);
	}

	public static Time ofYmd(String s) {
		return of(To.date(s).atStartOfDay());
	}

	public static Time ofYmdHms(String s) {
		return of(To.time(s));
	}

	public static Time of(int y, int m, int d) {
		return of(y, m, d, 0, 0);
	}

	public static Time of(int y, int m, int d, int h, int m_) {
		return of(LocalDateTime.of(y, m, d, h, m_));
	}

	public static Time ofEpochDay(long e) {
		return of(LocalDate.ofEpochDay(e).atStartOfDay());
	}

	public static Time ofEpochSec(long e) {
		return of(LocalDateTime.ofEpochSecond(e, 0, ZoneOffset.ofHours(timeZone)));
	}

	public static Time of(LocalDateTime dt) {
		return new Time(dt);
	}

	private Time(LocalDateTime dateTime) {
		this.dateTime = dateTime;
	}

	public Time addDays(long n) {
		return new Time(dateTime.plusDays(n));
	}

	public Time addHours(long n) {
		return new Time(dateTime.plusHours(n));
	}

	public Time addSeconds(long n) {
		return new Time(dateTime.plusSeconds(n));
	}

	public Time addYears(long n) {
		return new Time(dateTime.plusYears(n));
	}

	public String brief() {
		String df;
		if (dateTime.getSecond() != 0)
			df = "yyyy-MM-dd HH:mm:ss";
		else if (dateTime.getMinute() != 0)
			df = "yyyy-MM-dd HH:mm";
		else if (dateTime.getHour() != 0)
			df = "yyyy-MM-dd HH";
		else if (dateTime.getDayOfMonth() != 1)
			df = "yyyy-MM-dd";
		else if (dateTime.getMonth() != Month.JANUARY)
			df = "yyyy-MM";
		else
			df = "yyyy";
		return DateTimeFormatter.ofPattern(df, Locale.ENGLISH).format(dateTime);
	}

	public Time date() {
		return new Time(dateTime.toLocalDate().atStartOfDay());
	}

	public DayOfWeek dow() {
		return dateTime.getDayOfWeek();
	}

	public int dayOfMonth() {
		return dateTime.getDayOfMonth();
	}

	public long epochDay() {
		return dateTime.toLocalDate().toEpochDay();
	}

	public long epochSec() {
		return epochSec(timeZone);
	}

	public long epochSec(int timeZone) {
		return dateTime.toEpochSecond(ZoneOffset.ofHours(timeZone));
	}

	public int hhmm() {
		return dateTime.getHour() * 100 + dateTime.getMinute();
	}

	public int month() {
		return dateTime.getMonthValue();
	}

	public Time startOfDay() {
		return new Time(dateTime.toLocalDate().atStartOfDay());
	}

	public Time startOfMonth() {
		return new Time(dateTime.toLocalDate().withDayOfMonth(1).atStartOfDay());
	}

	public Time startOfYear() {
		return new Time(dateTime.toLocalDate().withMonth(1).withDayOfMonth(1).atStartOfDay());
	}

	public Time thisSecond() {
		return new Time(dateTime.withNano(0));
	}

	public int year() {
		return dateTime.getYear();
	}

	public String ymd() {
		return To.string(dateTime.toLocalDate());
	}

	public String ymdHms() {
		return To.string(dateTime);
	}

	@Override
	public int compareTo(Time other) {
		return compare(this, other);
	}

	@Override
	public boolean equals(Object object) {
		return Object_.clazz(object) == Time.class ? Objects.equals(dateTime, ((Time) object).dateTime) : false;
	}

	@Override
	public int hashCode() {
		return Objects.hashCode(dateTime);
	}

}
