package suite.trade.data;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiFunction;

import suite.primitive.adt.pair.LngFltPair;
import suite.streamlet.Outlet;
import suite.streamlet.Read;
import suite.streamlet.Streamlet;
import suite.trade.Time;
import suite.trade.TimeRange;
import suite.util.FunUtil.Fun;
import suite.util.Object_;
import suite.util.Set_;
import suite.util.String_;

public class StockHistory {

	private static Cleanse cleanse = new Cleanse();

	public final String exchange;
	public final Time time;
	public final Map<String, LngFltPair[]> data; // un-adjusted
	public final LngFltPair[] dividends;
	public final LngFltPair[] splits;

	public static StockHistory of(Outlet<String> outlet) {
		Map<String, String> properties = new HashMap<>();
		Map<String, LngFltPair[]> data = new HashMap<>();
		String line;

		while ('9' < (line = outlet.next()).charAt(0)) {
			String[] array = line.split("=");
			properties.put(array[0].trim(), array[1].trim());
		}
		String exchange = properties.get("exchange");
		String timeZoneStr = properties.get("timeZone");
		int timeZone = timeZoneStr != null ? Integer.parseInt(timeZoneStr) : 0;
		Time time = Time.ofYmdHms(line);
		LngFltPair[] dividends = readPairs(timeZone, outlet);
		LngFltPair[] splits = readPairs(timeZone, outlet);
		String tag;
		while ((tag = outlet.next()) != null)
			data.put(tag, readPairs(timeZone, outlet));
		return StockHistory.of(exchange, time, data, dividends, splits);
	}

	private static LngFltPair[] readPairs(int timeZone, Outlet<String> outlet) {
		List<LngFltPair> pairs = new ArrayList<>();
		String line;
		if (String_.equals(line = outlet.next(), "{"))
			while (!String_.equals(line = outlet.next(), "}")) {
				int p = line.lastIndexOf(":");
				Time time = Time.of(line.substring(0, p));
				float price = Float.parseFloat(line.substring(p + 1));
				pairs.add(LngFltPair.of(time.epochSec(timeZone), price));
			}
		else
			throw new RuntimeException();
		return pairs.toArray(new LngFltPair[0]);
	}

	public static StockHistory new_() {
		return of(null, TimeRange.min, new HashMap<>(), new LngFltPair[0], new LngFltPair[0]);
	}

	public static StockHistory of(Time time, Map<String, LngFltPair[]> data, LngFltPair[] dividends, LngFltPair[] splits) {
		return of(null, time, data, dividends, splits);
	}

	public static StockHistory of(//
			String exchange, //
			Time time, //
			Map<String, LngFltPair[]> data, //
			LngFltPair[] dividends, //
			LngFltPair[] splits) {
		return new StockHistory(exchange, time, data, dividends, splits);
	}

	private StockHistory(String exchange, Time time, Map<String, LngFltPair[]> data, LngFltPair[] dividends, LngFltPair[] splits) {
		this.exchange = exchange;
		this.time = time;
		this.data = data;
		this.dividends = dividends;
		this.splits = splits;
	}

	public LngFltPair[] get(String tag) {
		return data.getOrDefault(tag, new LngFltPair[0]);
	}

	public StockHistory cleanse() {
		return StockHistory.of(time, Read.from2(data).mapValue(cleanse::cleanse).toMap(), dividends, splits);
	}

	public StockHistory filter(TimeRange period) {
		long t0 = period.from.epochSec();
		long tx = period.to.epochSec();
		Fun<LngFltPair[], LngFltPair[]> filter_ = pairs0 -> {
			List<LngFltPair> pairs1 = new ArrayList<>();
			for (LngFltPair pair : pairs0)
				if (t0 <= pair.t0 && pair.t0 < tx)
					pairs1.add(pair);
			return pairs1.toArray(new LngFltPair[0]);
		};

		Map<String, LngFltPair[]> data1 = Read.from2(data) //
				.mapValue(filter_) //
				.toMap();

		return of(time, data1, filter_.apply(dividends), filter_.apply(splits));
	}

	public StockHistory merge(StockHistory other) {
		BiFunction<LngFltPair[], LngFltPair[], LngFltPair[]> merge_ = (pairs0, pairs1) -> {
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
		};
		Set<String> keys = Set_.union(data.keySet(), other.data.keySet());
		Map<String, LngFltPair[]> data1 = Read.from(keys) //
				.map2(key -> merge_.apply(get(key), other.get(key))) //
				.toMap();
		return of(exchange, time, data1, merge_.apply(dividends, other.dividends), merge_.apply(splits, other.splits));
	}

	public StockHistory alignToDate() {
		Fun<LngFltPair[], LngFltPair[]> align_ = pairs0 -> {
			List<LngFltPair> pairs1 = new ArrayList<>();
			Time date = TimeRange.min;
			for (LngFltPair pair : pairs0) {
				Time date1 = Time.ofEpochSec(pair.t0).startOfDay();
				if (Object_.compare(date, date1) < 0)
					pairs1.add(pair);
				date = date1;
			}
			return pairs1.toArray(new LngFltPair[0]);
		};
		Map<String, LngFltPair[]> data1 = Read.from2(data) //
				.mapValue(align_) //
				.toMap();
		return of(exchange, time, data1, align_.apply(dividends), align_.apply(splits));
	}

	public DataSource toDataSource() {
		LngFltPair[] openPairs = adjustPrices("open");
		LngFltPair[] closePairs = adjustPrices("close");
		LngFltPair[] lowPairs_ = adjustPrices("low");
		LngFltPair[] highPairs = adjustPrices("high");
		int closeLength = closePairs.length;
		long[] ts = new long[closeLength];
		float[] prices = new float[closeLength];
		float[] nextOpens = new float[closeLength];
		float[] nextLows_ = new float[closeLength];
		float[] nextHighs = new float[closeLength];
		int io = 0, il = 0, ih = 0;

		for (int ic = 0; ic < closeLength; ic++) {
			LngFltPair closePair = closePairs[ic];
			long t = closePair.t0;

			ts[ic] = t;
			prices[ic] = closePair.t1;
			nextOpens[ic] = ((io = scan(openPairs, io, t)) < openPairs.length ? openPairs[io] : closePair).t1;
			nextLows_[ic] = ((il = scan(lowPairs_, il, t)) < lowPairs_.length ? lowPairs_[il] : closePair).t1;
			nextHighs[ic] = ((ih = scan(highPairs, ih, t)) < highPairs.length ? highPairs[ih] : closePair).t1;
		}

		return DataSource.of(ts, prices, nextOpens, nextLows_, nextHighs);
	}

	private int scan(LngFltPair[] pairs, int i, long t) {
		int pength = pairs.length;
		while (i < pength && pairs[i].t0 < t)
			i++;
		return i;
	}

	public Streamlet<String> write() {
		Streamlet<String> s0 = Read.each( //
				"exchange = " + exchange, //
				"timeZone = 8", //
				time.ymdHms());
		Streamlet<String> s1 = Read.each(dividends, splits).concatMap(this::concat);
		Streamlet<String> s2 = Read.from2(data).concatMap((tag, fs) -> concat(fs).cons(tag));
		return Streamlet.concat(s0, s1, s2);
	}

	private Streamlet<String> concat(LngFltPair[] pairs) {
		return Streamlet.concat( //
				Read.each("{"), //
				Read.from(pairs).map(pair -> Time.ofEpochSec(pair.t0).ymdHms() + ":" + pair.t1), //
				Read.each("}"));
	}

	private LngFltPair[] adjustPrices(String tag) {
		LngFltPair[] pairs0 = data.get(tag);
		int length = pairs0.length;
		LngFltPair[] pairs1 = new LngFltPair[length];

		int si = splits.length - 1;
		int di = dividends.length - 1;
		float a = 0f, b = 1f;

		for (int i = length - 1; 0 <= i; i--) {
			LngFltPair pair = pairs0[i];
			long t = pair.t0;

			if (0 <= di) {
				LngFltPair dividend = dividends[di];
				if (t < dividend.t0) {
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
				if (t < split.t0) {
					b *= split.t1;
					si--;
				}
			}

			pairs1[i] = LngFltPair.of(pair.t0, a + b * pair.t1);
		}

		return pairs1;
	}

}
