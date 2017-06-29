package suite.trade.data;

import java.time.DayOfWeek;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Predicate;

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

	public static Time getOpenTimeBefore(Time time) {
		time = until(time, -1, HkexUtil::isMarketOpen_);
		time = until(time, -1, time_ -> !isMarketOpen_(time_));
		return time.addSeconds(1);
	}

	public static Time getCloseTimeBefore(Time time) {
		time = until(time, -1, time_ -> !isMarketOpen_(time_));
		time = until(time, -1, HkexUtil::isMarketOpen_);
		return time.addSeconds(1);
	}

	public static Time getTradeTimeBefore(Time time) {
		return until(time, -1, HkexUtil::isMarketOpen_);
	}

	public static Time getTradeTimeAfter(Time time) {
		return until(time, 1, HkexUtil::isMarketOpen_);
	}

	private static Time until(Time time, int dir, Predicate<Time> pred) {
		Time dt = time;
		if (!pred.test(dt)) {
			dt = dt.thisSecond().addSeconds(dir < 0 ? 0 : 1);
			Time dt1 = null;
			for (int d : new int[] { 14400, 3600, 300, 30, 5, 1, })
				while (!pred.test(dt1 = dt.addSeconds(dir * d)))
					dt = dt1;
			dt = dt1;
		}
		return dt;
	}

	private static boolean isMarketOpen_(Time time) {
		int hhmm = time.hhmm();
		return !weekends.contains(time.dow()) && 900 <= hhmm && hhmm < 1630;
	}

}
