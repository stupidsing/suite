package suite.trade;

import java.time.LocalDate;

public class Period {

	public final LocalDate frDate;
	public final LocalDate toDate;

	public static Period ages() {
		LocalDate frDate = LocalDate.of(1980, 1, 1);
		LocalDate toDate = LocalDate.of(2020, 1, 1);
		return of(frDate, toDate);
	}

	public static Period backTestDaysBefore(int alignment, int nDays) {

		// align date range boundaries to reduce number of web queries
		long todayEpochDay = LocalDate.now().toEpochDay();
		long endEpochDate = todayEpochDay - todayEpochDay % alignment;
		long startEpochDate = endEpochDate - nDays;
		return of(LocalDate.ofEpochDay(startEpochDate), LocalDate.ofEpochDay(endEpochDate));
	}

	public static Period daysBefore(int nDays) {
		LocalDate today = LocalDate.now();
		LocalDate frDate = today.minusDays(128);
		LocalDate toDate = today;
		return of(frDate, toDate);
	}

	public static Period threeYears() {
		return yearsBefore(-3);
	}

	public static Period fiveYears() {
		return yearsBefore(-5);
	}

	private static Period yearsBefore(int n) {
		LocalDate toDate = LocalDate.now().withDayOfMonth(1);
		LocalDate frDate = toDate.plusYears(n);
		return of(frDate, toDate);
	}

	public static Period of(LocalDate frDate, LocalDate toDate) {
		return new Period(frDate, toDate);
	}

	private Period(LocalDate frDate, LocalDate toDate) {
		this.frDate = frDate;
		this.toDate = toDate;
	}

}
