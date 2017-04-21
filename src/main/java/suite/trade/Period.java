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

	public static Period beforeToday(int nDays) {
		LocalDate today = LocalDate.now();
		LocalDate frDate = today.minusDays(128);
		LocalDate toDate = today;
		return of(frDate, toDate);
	}

	public static Period fiveYears() {
		LocalDate toDate = LocalDate.now().withDayOfMonth(1);
		LocalDate frDate = toDate.plusYears(-5);
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
