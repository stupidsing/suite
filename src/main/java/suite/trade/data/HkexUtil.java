package suite.trade.data;

import java.time.DayOfWeek;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import suite.trade.Time;
import suite.util.String_;

public class HkexUtil {

	private static Set<DayOfWeek> weekends = new HashSet<>(Arrays.asList(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY));

	public static boolean isMarketOpen(Time time) {
		return isMarketOpen_(time);
	}

	public static String toStockCode(String symbol) {
		return "" + Integer.parseInt(symbol.replace(".HK", ""));
	}

	public static String toSymbol(String stockCode) {
		return String_.right("0000" + stockCode.trim(), -4) + ".HK";
	}

	public static Time getTradeTimeBefore(Time time) {
		Time dt = time, dt1;
		if (!isMarketOpen_(dt)) {
			dt = dt.thisSecond();
			for (int d : new int[] { 14400, 3600, 300, 30, 5, })
				while (!isMarketOpen_(dt1 = dt.addSeconds(-d)))
					dt = dt1;
			while (!isMarketOpen_(dt))
				dt = dt.addSeconds(-1);
		}
		return dt;
	}

	public static Time getTradeTimeAfter(Time time) {
		Time dt = time, dt1;
		if (!isMarketOpen_(dt)) {
			dt = dt.thisSecond().addSeconds(1);
			for (int d : new int[] { 14400, 3600, 300, 30, 5, })
				while (!isMarketOpen_(dt1 = dt.addSeconds(d)))
					dt = dt1;
			while (!isMarketOpen_(dt))
				dt = dt.addSeconds(1);
		}
		return dt;
	}

	private static boolean isMarketOpen_(Time time) {
		int hhmm = time.hhmm();
		return !weekends.contains(time.dow()) && 900 <= hhmm && hhmm < 1630;
	}

}
