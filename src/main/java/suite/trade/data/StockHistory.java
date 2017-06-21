package suite.trade.data;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import suite.adt.pair.LngFltPair;
import suite.streamlet.Outlet;
import suite.streamlet.Read;
import suite.streamlet.Streamlet;
import suite.trade.Time;
import suite.trade.TimeRange;
import suite.util.Object_;
import suite.util.Set_;
import suite.util.String_;

public class StockHistory {

	public final Time time;
	public final Map<String, LngFltPair[]> data; // un-adjusted
	public final LngFltPair[] dividends;
	public final LngFltPair[] splits;

	public static StockHistory of(Outlet<String> outlet) {
		Time time = Time.ofYmdHms(outlet.next());
		LngFltPair[] dividends = readPairs(outlet);
		LngFltPair[] splits = readPairs(outlet);
		Map<String, LngFltPair[]> data = new HashMap<>();
		String tag;
		while ((tag = outlet.next()) != null)
			data.put(tag, readPairs(outlet));
		return StockHistory.of(time, data, dividends, splits);
	}

	private static LngFltPair[] readPairs(Outlet<String> outlet) {
		List<LngFltPair> pairs = new ArrayList<>();
		String line;
		if (String_.equals(line = outlet.next(), "{"))
			while (!String_.equals(line = outlet.next(), "}")) {
				int p = line.lastIndexOf(":");
				Time date = Time.of(line.substring(0, p));
				float price = Float.parseFloat(line.substring(p + 1));
				pairs.add(LngFltPair.of(date.epochUtcSecond(), price));
			}
		else
			throw new RuntimeException();
		return pairs.toArray(new LngFltPair[0]);
	}

	public static StockHistory new_() {
		return of(TimeRange.ages().from, new HashMap<>(), new LngFltPair[0], new LngFltPair[0]);
	}

	public static StockHistory of(Map<String, LngFltPair[]> data, LngFltPair[] dividends, LngFltPair[] splits) {
		return of(HkexUtil.getTradeTimeBefore(Time.now()), data, dividends, splits);
	}

	public static StockHistory of(Time time, Map<String, LngFltPair[]> data, LngFltPair[] dividends, LngFltPair[] splits) {
		return new StockHistory(time, data, dividends, splits);
	}

	private StockHistory(Time time, Map<String, LngFltPair[]> data, LngFltPair[] dividends, LngFltPair[] splits) {
		this.time = time;
		this.data = data;
		this.dividends = dividends;
		this.splits = splits;
	}

	public LngFltPair[] get(String tag) {
		return data.getOrDefault(tag, new LngFltPair[0]);
	}

	public StockHistory merge(StockHistory other) {
		Set<String> keys = Set_.union(data.keySet(), other.data.keySet());
		Map<String, LngFltPair[]> data1 = Read.from(keys) //
				.map2(key -> merge(get(key), other.get(key))) //
				.toMap();
		return of(data1, merge(dividends, other.dividends), merge(splits, other.splits));
	}

	public StockHistory alignToDate() {
		Map<String, LngFltPair[]> data1 = Read.from2(data) //
				.mapValue(this::alignToDate) //
				.toMap();
		return of(data1, alignToDate(dividends), alignToDate(splits));
	}

	private LngFltPair[] merge(LngFltPair[] pairs0, LngFltPair[] pairs1) {
		List<LngFltPair> pairs = new ArrayList<>();
		int length1 = pairs1.length;
		int i1 = 0;
		for (LngFltPair pair0 : pairs0) {
			long l0 = pair0.t0;
			while (i1 < length1) {
				LngFltPair pair1 = pairs1[i1];
				long l1 = pair1.t0;
				if (l1 < l0)
					pairs.add(pair1);
				else if (l0 < l1)
					break;
				i1++;
			}
			pairs.add(pair0);
		}
		while (i1 < length1)
			pairs.add(pairs1[i1++]);
		return pairs.toArray(new LngFltPair[0]);
	}

	private LngFltPair[] alignToDate(LngFltPair[] pairs) {
		List<LngFltPair> list = new ArrayList<>();
		Time date = TimeRange.ages().from;
		for (LngFltPair pair : pairs) {
			Time date1 = Time.ofEpochUtcSecond(pair.t0).startOfDay();
			if (Object_.compare(date, date1) < 0)
				list.add(pair);
			date = date1;
		}
		return list.toArray(new LngFltPair[0]);
	}

	public DataSource adjustPrices(String tag) {
		LngFltPair[] pairs = data.get(tag);
		int length = pairs.length;
		String[] dates = new String[length];
		float[] prices = new float[length];

		int si = splits.length - 1;
		int di = dividends.length - 1;
		float a = 0f, b = 1f;

		for (int i = length - 1; 0 <= i; i--) {
			LngFltPair pair = pairs[i];
			long epoch = pair.t0;

			if (0 <= di) {
				LngFltPair dividend = dividends[di];
				if (epoch < dividend.t0) {
					if (Boolean.TRUE)
						// may got negative prices
						a -= dividend.t1 * b;
					else
						// may got skewed profits
						b *= (pair.t0 - dividend.t0) / pair.t0;
					di--;
				}
			}

			if (0 <= si) {
				LngFltPair split = splits[si];
				if (epoch < split.t0) {
					b *= split.t1;
					si--;
				}
			}

			dates[i] = Time.ofEpochUtcSecond(pair.t0).ymd();
			prices[i] = a + b * pair.t1;
		}

		return new DataSource(dates, prices);
	}

	public Streamlet<String> write() {
		Streamlet<String> s0 = Read.each(time.ymdHms());
		Streamlet<String> s1 = Read.each(dividends, splits).concatMap(this::concat);
		Streamlet<String> s2 = Read.from2(data).concatMap((tag, fs) -> concat(fs).cons(tag));
		return Streamlet.concat(s0, s1, s2);
	}

	private Streamlet<String> concat(LngFltPair[] pairs) {
		return Streamlet.concat( //
				Read.each("{"), //
				Read.from(pairs).map(pair -> Time.ofEpochUtcSecond(pair.t0).ymdHms() + ":" + pair.t1), //
				Read.each("}"));
	}

}
