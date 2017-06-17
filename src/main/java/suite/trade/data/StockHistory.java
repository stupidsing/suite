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
import suite.util.FunUtil.Source;
import suite.util.Set_;
import suite.util.String_;

public class StockHistory {

	public final Map<String, LngFltPair[]> data; // un-adjusted
	public final LngFltPair[] dividends;
	public final LngFltPair[] splits;

	public static StockHistory of(Outlet<String> outlet) {
		Source<String> source = outlet.source();
		LngFltPair[] dividends = readPairs(source);
		LngFltPair[] splits = readPairs(source);
		Map<String, LngFltPair[]> data = new HashMap<>();
		String tag;
		if ((tag = source.source()) != null)
			data.put(tag, readPairs(source));
		return StockHistory.of(data, dividends, splits);
	}

	private static LngFltPair[] readPairs(Source<String> source) {
		List<LngFltPair> pairs = new ArrayList<>();
		String line;
		if (String_.equals(line = source.source(), "{"))
			while (!String_.equals(line = source.source(), "}")) {
				String[] array = line.split(":");
				pairs.add(LngFltPair.of(Long.parseLong(array[0]), Float.parseFloat(array[1])));
			}
		return pairs.toArray(new LngFltPair[0]);
	}

	public static StockHistory new_() {
		return of(new HashMap<>(), new LngFltPair[0], new LngFltPair[0]);
	}

	public static StockHistory of(Map<String, LngFltPair[]> data, LngFltPair[] dividends, LngFltPair[] splits) {
		return new StockHistory(data, dividends, splits);
	}

	private StockHistory(Map<String, LngFltPair[]> data, LngFltPair[] dividends, LngFltPair[] splits) {
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

			if (0 <= si) {
				LngFltPair split = splits[si];
				if (epoch < split.t0) {
					a *= split.t1;
					b *= split.t1;
					si--;
				}
			}

			if (0 <= di) {
				LngFltPair dividend = dividends[di];
				if (epoch < dividend.t0) {
					a -= dividend.t1;
					di--;
				}
			}

			dates[i] = Time.ofEpochUtcSecond(pair.t0).ymd();
			prices[i] = a + b * pair.t1;
		}

		return new DataSource(dates, prices);
	}

	public Streamlet<String> write() {
		Streamlet<String> s0 = Read.each(dividends, splits) //
				.concatMap(this::concat);

		Streamlet<String> s1 = Read.each("open", "close", "high", "low") //
				.concatMap(tag -> concat(data.get(tag)).cons(tag));

		return Streamlet.concat(s0, s1);
	}

	private Streamlet<String> concat(LngFltPair[] pairs) {
		return Streamlet.concat( //
				Read.each("{"), //
				Read.from(pairs).map(pair -> pair.t0 + ":" + pair.t1), //
				Read.each("}"));
	}

}
