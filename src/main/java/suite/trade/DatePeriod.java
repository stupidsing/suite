package suite.trade;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import suite.adt.Range;

public class DatePeriod extends Range<LocalDate> {

	public static DatePeriod ages() {
		LocalDate frDate = LocalDate.of(1980, 1, 1);
		LocalDate toDate = LocalDate.of(2020, 1, 1);
		return of_(frDate, toDate);
	}

	// align date range boundaries to reduce number of web queries (and
	// calculations)
	public static DatePeriod backTestDaysBefore(LocalDate date, int nDays, int alignment) {
		return backTestDaysBefore_(date, date.plusDays(1), nDays, alignment).get(0);
	}

	public static List<DatePeriod> backTestDaysBefore(DatePeriod period, int nDays, int alignment) {
		return backTestDaysBefore_(period.from, period.to, nDays, alignment);
	}

	public static DatePeriod daysBefore(LocalDate to, int n) {
		return daysBefore_(to, n);
	}

	public static DatePeriod daysBefore(int nDays) {
		return daysBefore_(LocalDate.now(), nDays);
	}

	public static DatePeriod threeYears() {
		return yearsBefore_(3);
	}

	public static DatePeriod fiveYears() {
		return yearsBefore_(5);
	}

	public static DatePeriod of(LocalDate from, LocalDate to) {
		return of_(from, to);
	}

	public static DatePeriod ofYear(int year) {
		return of_(LocalDate.of(year, 1, 1), LocalDate.of(year + 1, 1, 1));
	}

	public static DatePeriod yearsBefore(LocalDate to, int n) {
		return yearsBefore_(to, n);
	}

	private static List<DatePeriod> backTestDaysBefore_(LocalDate frDate, LocalDate toDate, int nDays, int alignment) {
		long epochDate0 = frDate.toEpochDay() % alignment;
		long epochDatex = toDate.minusDays(1).toEpochDay() % alignment;
		List<DatePeriod> periods = new ArrayList<>();
		while (epochDate0 <= epochDatex) {
			periods.add(daysBefore_(LocalDate.ofEpochDay(epochDate0), nDays));
			epochDate0 += alignment;
		}
		return periods;
	}

	private static DatePeriod daysBefore_(LocalDate to, int n) {
		return of_(to.minusDays(n), to);
	}

	private static DatePeriod yearsBefore_(int n) {
		return yearsBefore_(LocalDate.now().withDayOfMonth(1), n);
	}

	private static DatePeriod yearsBefore_(LocalDate to, int n) {
		return of_(to.minusYears(n), to);
	}

	private static DatePeriod of_(LocalDate from, LocalDate to) {
		return new DatePeriod(from, to);
	}

	private DatePeriod(LocalDate from, LocalDate to) {
		super(from, to);
	}

	public boolean contains(LocalDate date) {
		return from.compareTo(date) <= 0 && date.compareTo(to) < 0;
	}

}
