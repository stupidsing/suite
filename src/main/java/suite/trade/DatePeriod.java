package suite.trade;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import suite.adt.Range;
import suite.streamlet.Read;
import suite.streamlet.Streamlet;
import suite.util.Object_;

public class DatePeriod extends Range<LocalDate> {

	public static DatePeriod ages() {
		LocalDate frDate = LocalDate.of(1980, 1, 1);
		LocalDate toDate = LocalDate.of(2020, 1, 1);
		return of_(frDate, toDate);
	}

	// align date range boundaries to reduce number of web queries (and
	// calculations)
	public static DatePeriod backTestDaysBefore(LocalDate date, int nDays, int alignment) {
		return backTestDaysBefore_(date, date.plusDays(1), nDays, alignment).uniqueResult();
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

	public static DatePeriod of(List<LocalDate> dates) {
		LocalDate frDate = LocalDate.MAX;
		LocalDate toDate = LocalDate.MIN;
		for (LocalDate date : dates) {
			frDate = frDate.compareTo(date) < 0 ? frDate : date;
			toDate = toDate.compareTo(date) < 0 ? date : toDate;
		}
		return of_(frDate, toDate);
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

	private static DatePeriod yearsBefore_(int n) {
		return yearsBefore_(LocalDate.now().withDayOfMonth(1), n);
	}

	private static DatePeriod yearsBefore_(LocalDate to, int n) {
		return of_(to.minusYears(n), to);
	}

	public Streamlet<DatePeriod> backTestDaysBefore(int nDays, int alignment) {
		return backTestDaysBefore_(from, to, nDays, alignment);
	}

	public boolean contains(LocalDate date) {
		return from.compareTo(date) <= 0 && date.compareTo(to) < 0;
	}

	@Override
	public boolean equals(Object object) {
		if (Object_.clazz(object) == DatePeriod.class) {
			DatePeriod other = (DatePeriod) object;
			return Objects.equals(from, other.from) && Objects.equals(to, other.to);
		} else
			return false;
	}

	public DatePeriod plusDays(int n) {
		return of_(from.plusDays(n), to.plusDays(n));
	}

	private static Streamlet<DatePeriod> backTestDaysBefore_(LocalDate frDate, LocalDate toDate, int nDays, int alignment) {
		long epochDate0 = frDate.toEpochDay();
		long epochDate1 = toDate.toEpochDay() - 1;
		epochDate0 -= epochDate0 % alignment;
		epochDate1 -= epochDate1 % alignment;
		List<DatePeriod> periods = new ArrayList<>();
		while (epochDate0 <= epochDate1) {
			periods.add(daysBefore_(LocalDate.ofEpochDay(epochDate0), nDays));
			epochDate0 += alignment;
		}
		return Read.from(periods);
	}

	private static DatePeriod daysBefore_(LocalDate to, int n) {
		return of_(to.minusDays(n), to);
	}

	private static DatePeriod of_(LocalDate from, LocalDate to) {
		return new DatePeriod(from, to);
	}

	private DatePeriod(LocalDate from, LocalDate to) {
		super(from, to);
	}

}
