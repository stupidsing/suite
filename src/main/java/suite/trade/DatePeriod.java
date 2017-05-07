package suite.trade;

import java.time.LocalDate;

import suite.adt.Range;

public class DatePeriod extends Range<LocalDate> {

	public static DatePeriod ages() {
		LocalDate from = LocalDate.of(1980, 1, 1);
		LocalDate to = LocalDate.of(2020, 1, 1);
		return of(from, to);
	}

	public static DatePeriod backTestDaysBefore(LocalDate date, int nDays, int alignment) {

		// align date range boundaries to reduce number of web queries (and
		// calculations)
		LocalDate toDate = date.minusDays(date.toEpochDay() % alignment);
		LocalDate frDate = toDate.minusDays(nDays);
		return of(frDate, toDate);
	}

	public static DatePeriod daysBefore(int nDays) {
		LocalDate today = LocalDate.now();
		LocalDate from = today.minusDays(128);
		LocalDate to = today;
		return of(from, to);
	}

	public static DatePeriod threeYears() {
		return yearsBefore(-3);
	}

	public static DatePeriod fiveYears() {
		return yearsBefore(-5);
	}

	private static DatePeriod yearsBefore(int n) {
		LocalDate to = LocalDate.now().withDayOfMonth(1);
		LocalDate from = to.plusYears(n);
		return of(from, to);
	}

	public static DatePeriod of(LocalDate from, LocalDate to) {
		return new DatePeriod(from, to);
	}

	private DatePeriod(LocalDate from, LocalDate to) {
		super(from, to);
	}

	public double nYears() {
		return (to.toEpochDay() - from.toEpochDay()) / 365f;
	}

}
