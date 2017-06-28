package suite.trade;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import suite.adt.Range;
import suite.streamlet.Read;
import suite.streamlet.Streamlet;
import suite.util.Object_;

public class TimeRange extends Range<Time> {

	public static Time min = Time.of(1980, 1, 1);
	public static Time max = Time.of(2020, 1, 1);

	public static TimeRange ages() {
		return of_(min, max);
	}

	// align date range boundaries to reduce number of web queries (and
	// calculations)
	public static TimeRange backTestDaysBefore(Time dt, int nDays, int alignment) {
		return backTestDaysBefore_(dt, dt.addDays(1), nDays, alignment).uniqueResult();
	}

	public static TimeRange daysBefore(Time to, int n) {
		return daysBefore_(to, n);
	}

	public static TimeRange daysBefore(int nDays) {
		return daysBefore_(Time.today(), nDays);
	}

	public static TimeRange threeYears() {
		return yearsBefore_(3);
	}

	public static TimeRange fiveYears() {
		return yearsBefore_(5);
	}

	public static TimeRange of(Time from, Time to) {
		return of_(from, to);
	}

	public static TimeRange ofYear(int year) {
		return of_(Time.of(year, 1, 1), Time.of(year + 1, 1, 1));
	}

	public static TimeRange rangeOf(List<Time> ts) {
		Time frDt = min;
		Time toDt = max;
		for (Time t : ts) {
			frDt = frDt.compareTo(t) < 0 ? frDt : t;
			toDt = toDt.compareTo(t) < 0 ? t : toDt;
		}
		return of_(frDt, toDt.addDays(1));
	}

	public static TimeRange yearsBefore(Time to, int n) {
		return yearsBefore_(to, n);
	}

	private static TimeRange yearsBefore_(int n) {
		return yearsBefore_(Time.now().startOfMonth(), n);
	}

	private static TimeRange yearsBefore_(Time to, int n) {
		return of_(to.addYears(-n), to);
	}

	public TimeRange addDays(int n) {
		return of_(from.addDays(n), to.addDays(n));
	}

	public Streamlet<TimeRange> backTestDaysBefore(int nDays, int alignment) {
		return backTestDaysBefore_(from, to, nDays, alignment);
	}

	@Override
	public boolean equals(Object object) {
		if (Object_.clazz(object) == TimeRange.class) {
			TimeRange other = (TimeRange) object;
			return Objects.equals(from, other.from) && Objects.equals(to, other.to);
		} else
			return false;
	}

	private static Streamlet<TimeRange> backTestDaysBefore_(Time frDate, Time toDate, int nDays, int alignment) {
		long epochDate0 = frDate.epochDay();
		long epochDate1 = toDate.epochDay() - 1;
		epochDate0 -= epochDate0 % alignment;
		epochDate1 -= epochDate1 % alignment;
		List<TimeRange> periods = new ArrayList<>();
		while (epochDate0 <= epochDate1) {
			periods.add(daysBefore_(Time.ofEpochDay(epochDate0), nDays));
			epochDate0 += alignment;
		}
		return Read.from(periods);
	}

	private static TimeRange daysBefore_(Time to, int n) {
		return of_(to.addDays(-n), to);
	}

	private static TimeRange of_(Time from, Time to) {
		return new TimeRange(from, to);
	}

	private TimeRange(Time from, Time to) {
		super(from, to);
	}

}
