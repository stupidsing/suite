package suite.trade.data;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import suite.util.String_;

public class HkexUtil {

	private static Set<DayOfWeek> weekends = new HashSet<>(Arrays.asList(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY));

	public static boolean isMarketOpen(LocalDateTime time) {
		return isMarketOpen_(time);
	}

	public static String toStockCode(String symbol) {
		return "" + Integer.parseInt(symbol.replace(".HK", ""));
	}

	public static String toSymbol(String stockCode) {
		return String_.right("0000" + stockCode.trim(), -4) + ".HK";
	}

	public static LocalDate getPreviousTradeDate(LocalDateTime time) {
		return getTradeTimeBefore(time).toLocalDate();
	}

	public static LocalDateTime getTradeTimeAfter(LocalDateTime time) {
		LocalDateTime dt = time;
		while (!isMarketOpen_(dt))
			dt = dt.plusHours(1);
		return dt;
	}

	private static LocalDateTime getTradeTimeBefore(LocalDateTime time) {
		LocalDateTime dt = time;
		while (!isMarketOpen_(dt))
			dt = dt.minusHours(1);
		return dt;
	}

	private static boolean isMarketOpen_(LocalDateTime time) {
		int hhmm = time.getHour() * 100 + time.getMinute();
		return !weekends.contains(time.getDayOfWeek()) && 900 <= hhmm && hhmm < 1630;
	}

}
