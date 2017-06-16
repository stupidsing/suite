package suite.trade.data;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import suite.adt.pair.LngFltPair;
import suite.trade.Time;
import suite.util.FunUtil.Sink;
import suite.util.FunUtil.Source;
import suite.util.String_;
import suite.util.To;

public class StockHistory {

	public final LngFltPair[] prices0; // un-adjusted
	public final LngFltPair[] dividends;
	public final LngFltPair[] splits;

	public static StockHistory of(Source<String> source) {
		LngFltPair[] prices0 = readPairs(source);
		LngFltPair[] dividends = readPairs(source);
		LngFltPair[] splits = readPairs(source);
		return StockHistory.of(prices0, dividends, splits);
	}

	private static LngFltPair[] readPairs(Source<String> source) {
		List<LngFltPair> pairs = new ArrayList<>();
		String line;
		if (String_.equals(line = source.source(), "{"))
			while ((line = source.source()) != null) {
				if (!String_.equals(line = source.source(), "}")) {
					String[] array = line.split(":");
					pairs.add(LngFltPair.of(Long.parseLong(array[0]), Float.parseFloat(array[1])));
				}
			}
		return pairs.toArray(new LngFltPair[0]);
	}

	public static StockHistory of(LngFltPair[] prices0, LngFltPair[] dividends, LngFltPair[] splits) {
		return new StockHistory(prices0, dividends, splits);
	}

	private StockHistory(LngFltPair[] prices0, LngFltPair[] dividends, LngFltPair[] splits) {
		this.prices0 = prices0;
		this.dividends = dividends;
		this.splits = splits;
	}

	public StockHistory merge(StockHistory other) {
		return of( //
				merge(prices0, other.prices0), //
				merge(dividends, other.dividends), //
				merge(splits, other.splits));
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

	public DataSource adjust() {
		int length = prices0.length;
		String[] dates = To.array(String.class, length, i -> Time.ofEpochUtcSecond(prices0[i].t0).ymd());
		float[] prices = To.arrayOfFloats(prices0, pair -> pair.t1);

		int di = dividends.length - 1;
		int si = splits.length - 1;
		float a = 0f, b = 1f;

		for (int i = length - 1; 0 <= i; i--) {
			prices[i] = a + b * prices[i];
			long epoch = prices0[i].t0;

			if (0 <= di) {
				LngFltPair dividend = dividends[di];
				if (epoch == dividend.t0) {
					a -= dividend.t1;
					di--;
				}
			}

			if (0 <= si) {
				LngFltPair split = splits[si];
				if (epoch == split.t0) {
					a *= split.t1;
					b *= split.t1;
					si--;
				}
			}
		}

		return new DataSource(dates, prices);
	}

	public void write(Sink<String> sink) {
		for (LngFltPair[] pairs : Arrays.asList(prices0, dividends, splits)) {
			sink.sink("{");
			for (LngFltPair pair : pairs)
				sink.sink(pair.t0 + ":" + pair.t1);
			sink.sink("{");
		}
	}

}
