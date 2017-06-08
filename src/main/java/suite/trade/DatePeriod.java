package suite.trade;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import suite.adt.Range;
import suite.streamlet.Read;
import suite.streamlet.Streamlet;
import suite.util.Object_;

public class DatePeriod extends Range<Time> {

	public static DatePeriod ages() {
		Time frDate = Time.of(1980, 1, 1);
		Time toDate = Time.of(2020, 1, 1);
		return of_(frDate, toDate);
	}

	// align date range boundaries to reduce number of web queries (and
	// calculations)
	public static DatePeriod backTestDaysBefore(Time dt, int nDays, int alignment) {
		return backTestDaysBefore_(dt, dt.addDays(1), nDays, alignment).uniqueResult();
	}

	public static DatePeriod daysBefore(Time to, int n) {
		return daysBefore_(to, n);
	}

	public static DatePeriod daysBefore(int nDays) {
		return daysBefore_(Time.today(), nDays);
	}

	public static DatePeriod threeYears() {
		return yearsBefore_(3);
	}

	public static DatePeriod fiveYears() {
		return yearsBefore_(5);
	}

	public static DatePeriod ofDateTimes(List<Time> ts) {
		Time frDt = Time.MAX;
		Time toDt = Time.MIN;
		for (Time t : ts) {
			frDt = frDt.compareTo(t) < 0 ? frDt : t;
			toDt = toDt.compareTo(t) < 0 ? t : toDt;
		}
		return of_(frDt, toDt.addDays(1));
	}

	public static DatePeriod of(Time from, Time to) {
		return of_(from, to);
	}

	public static DatePeriod ofYear(int year) {
		return of_(Time.of(year, 1, 1), Time.of(year + 1, 1, 1));
	}

	public static DatePeriod yearsBefore(Time to, int n) {
		return yearsBefore_(to, n);
	}

	private static DatePeriod yearsBefore_(int n) {
		return yearsBefore_(Time.now().startOfMonth(), n);
	}

	private static DatePeriod yearsBefore_(Time to, int n) {
		return of_(to.addYears(n), to);
	}

	public Streamlet<DatePeriod> backTestDaysBefore(int nDays, int alignment) {
		return backTestDaysBefore_(from, to, nDays, alignment);
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
		return of_(from.addDays(n), to.addDays(n));
	}

	private static Streamlet<DatePeriod> backTestDaysBefore_(Time frDate, Time toDate, int nDays, int alignment) {
		long epochDate0 = frDate.epochDay();
		long epochDate1 = toDate.epochDay() - 1;
		epochDate0 -= epochDate0 % alignment;
		epochDate1 -= epochDate1 % alignment;
		List<DatePeriod> periods = new ArrayList<>();
		while (epochDate0 <= epochDate1) {
			periods.add(daysBefore_(Time.ofEpochDay(epochDate0), nDays));
			epochDate0 += alignment;
		}
		return Read.from(periods);
	}

	private static DatePeriod daysBefore_(Time to, int n) {
		return of_(to.addDays(-n), to);
	}

	private static DatePeriod of_(Time from, Time to) {
		return new DatePeriod(from, to);
	}

	private DatePeriod(Time from, Time to) {
		super(from, to);
	}

}
