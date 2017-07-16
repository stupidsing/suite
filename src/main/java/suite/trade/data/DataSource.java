package suite.trade.data;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import suite.math.stat.TimeSeries;
import suite.primitive.Floats_;
import suite.primitive.FltPrimitives.Obj_Flt;
import suite.primitive.LngPrimitives.Obj_Lng;
import suite.primitive.Longs.LongsBuilder;
import suite.primitive.adt.pair.LngFltPair;
import suite.primitive.streamlet.LngStreamlet;
import suite.streamlet.As;
import suite.streamlet.Read;
import suite.streamlet.Streamlet;
import suite.streamlet.Streamlet2;
import suite.trade.Time;
import suite.trade.TimeRange;
import suite.trade.Trade_;
import suite.util.Object_;
import suite.util.Set_;
import suite.util.To;

public class DataSource {

	private static long tickDuration = 7l * 3600l; // 09:30 to 16:30
	private static Cleanse cleanse = new Cleanse();
	private static TimeSeries timeSeries = new TimeSeries();

	public final long[] ts;
	public final float[] prices;
	public final float[] opens;
	public final float[] closes;
	public final float[] lows;
	public final float[] highs;

	public static class Datum {
		public final float open;
		public final float close;
		public final float low;
		public final float high;

		private static Datum of(float price) {
			return new Datum(price, price, price, price);
		}

		private Datum(float open, float close, float low, float high) {
			this.open = open;
			this.close = close;
			this.high = high;
			this.low = low;
		}
	}

	public static class Eod {
		public final float price;
		public final float nextOpen;
		public final float nextLow;
		public final float nextHigh;

		public static Eod of(float price) {
			return new Eod(price, price, price, price);
		}

		private Eod(float price, float nextOpen, float nextLow, float nextHigh) {
			this.price = price;
			this.nextOpen = nextOpen;
			this.nextLow = nextLow;
			this.nextHigh = nextHigh;
		}
	}

	public static <K> AlignKeyDataSource<K> alignAll(Streamlet2<K, DataSource> dsByKey0) {
		AlignDataSource alignDataSource = alignAll(dsByKey0.values());

		return dsByKey0 //
				.mapValue(alignDataSource::align) //
				.collect(As::streamlet2) //
				.apply(st -> new AlignKeyDataSource<>(alignDataSource.ts, st));
	}

	public static class AlignKeyDataSource<K> {
		public final long[] ts;
		public final Streamlet2<K, DataSource> dsByKey;

		public AlignKeyDataSource(long[] ts, Streamlet2<K, DataSource> dsByKey) {
			this.ts = ts;
			this.dsByKey = dsByKey;
		}
	}

	public static AlignDataSource alignAll(Streamlet<DataSource> dataSources) {
		Streamlet<Long> tradeTimes;
		if (Boolean.TRUE)
			tradeTimes = dataSources // union
					.concatMap(ds -> LngStreamlet.of(ds.ts).map(t -> t)) //
					.distinct();
		else
			tradeTimes = Read.from(Set_.intersect(dataSources // intersect
					.<Collection<Long>> map(ds -> LngStreamlet.of(ds.ts).map(t -> t).toList()) //
					.toList()));
		return new AlignDataSource(tradeTimes //
				.sort(Object_::compare) //
				.collect(Obj_Lng.lift(t -> t)) //
				.toArray());
	}

	public static class AlignDataSource {
		public final long[] ts;

		private AlignDataSource(long[] ts) {
			this.ts = ts;
		}

		public DataSource align(DataSource ds) {
			return ds.alignBeforePrices(ts);
		}
	}

	public static DataSource of(long[] ts, Streamlet<Datum> datums) {
		float[] ops = datums.collect(Obj_Flt.lift(pair -> pair.open)).toArray();
		float[] cls = datums.collect(Obj_Flt.lift(pair -> pair.close)).toArray();
		float[] los = datums.collect(Obj_Flt.lift(pair -> pair.low)).toArray();
		float[] his = datums.collect(Obj_Flt.lift(pair -> pair.high)).toArray();
		return ofOhlc(ts, ops, cls, los, his);
	}

	public static DataSource of(long[] ts, float[] prices) {
		return ofOhlc(ts, prices, prices, prices, prices);
	}

	// at the end of the day -
	// current price = today's closing price;
	// next price = tomorrow's opening price.
	public static DataSource ofOhlc(long[] ts, float[] opens, float[] closes, float[] lows, float[] highs) {
		return new DataSource(ts, opens, closes, lows, highs);
	}

	private DataSource(long[] ts, float[] opens, float[] closes, float[] lows, float[] highs) {
		super();
		this.ts = ts;
		this.prices = closes;
		this.opens = opens;
		this.closes = closes;
		this.lows = lows;
		this.highs = highs;
		if (ts.length != prices.length //
				|| ts.length != opens.length || ts.length != closes.length //
				|| ts.length != lows.length || ts.length != highs.length)
			throw new RuntimeException("mismatched dates and prices");
	}

	public DataSource after(Time time) {
		return range_(TimeRange.of(time, TimeRange.max));
	}

	public DataSource alignAfterPrices(long[] ts1) {
		int length0 = ts.length;
		int length1 = ts1.length;
		Datum[] data = new Datum[length1];
		int si = length0 - 1;

		for (int di = length1 - 1; 0 <= di; di--) {
			int si_ = si;
			long t1 = ts1[di];
			while (0 <= si && t1 <= ts[si])
				si--;
			data[di] = getDatum(si + 1, si_ + 1);
		}

		return of(ts1, Read.from(data));
	}

	public DataSource alignBeforePrices(long[] ts1) {
		int length0 = ts.length;
		int length1 = ts1.length;
		Datum[] data = new Datum[length1];
		int si = 0;

		for (int di = 0; di < length1; di++) {
			int si_ = si;
			long t1 = ts1[di];
			while (si < length0 && ts[si] + tickDuration <= t1 + tickDuration)
				si++;
			data[di] = getDatum(si_, si);
		}

		return of(ts1, Read.from(data));
	}

	public DataSource cleanse() {
		cleanse.cleanse(prices);
		cleanse.cleanse(opens);
		cleanse.cleanse(closes);
		cleanse.cleanse(lows);
		cleanse.cleanse(highs);
		return this;
	}

	public DataSource cons(long time, float price) {
		int length = ts.length;
		long[] ts1 = Arrays.copyOf(ts, length + 1);
		ts1[length] = time;
		return ofOhlc(ts1, //
				Floats_.concat(opens, new float[] { price, }), //
				Floats_.concat(closes, new float[] { price, }), //
				Floats_.concat(lows, new float[] { price, }), //
				Floats_.concat(highs, new float[] { price, }));
	}

	public LngFltPair first() {
		return get(0);
	}

	public LngFltPair get(int pos) {
		return get_(pos);
	}

	public Eod getEod(int pos) {
		int pos1 = pos + 1;
		if (pos1 < ts.length)
			return new Eod(prices[pos], opens[pos1], lows[pos1], highs[pos1]);
		else
			return Eod.of(prices[pos]);
	}

	public LngFltPair last() {
		return get(-1);
	}

	public LngFltPair last(Time time) {
		long t = time.epochSec();
		for (int i = ts.length - 1; 0 <= i; i--)
			if (ts[i] <= t)
				return LngFltPair.of(ts[i], prices[i]);
		return null;
	}

	public double lastReturn(int index) {
		double price0 = prices[index - 2];
		double price1 = prices[index - 1];
		return (price1 - price0) / price0;
	}

	public DataSource range(TimeRange period) {
		return range_(period);
	}

	public DataSource range(long t0, long tx) {
		return range_(t0, tx);
	}

	public String recent(String prefix, int size) {
		StringBuilder sb = new StringBuilder();
		for (int i = ts.length - size; i < ts.length; i++)
			sb.append(prefix + "[" + Time.ofEpochSec(ts[i]) + "] = " + To.string(prices[i]) + "\n");
		return sb.toString();
	}

	public float[] returns() {
		return timeSeries.returns(prices);
	}

	public void validate() {
		validate(prices);
		for (float[] prices1 : Arrays.asList(opens, closes, lows, highs))
			if (prices != prices1)
				validate(prices1);
	}

	@Override
	public String toString() {
		String range = 0 < prices.length ? first() + "~" + last() : "";
		return getClass().getSimpleName() + "(" + range + ")";
	}

	private void validate(float[] prices_) {
		int length = prices_.length;
		long t0 = 0 < length ? ts[0] : Long.MIN_VALUE;
		float price0 = 0 < length ? prices_[0] : Float.MAX_VALUE;
		String date0 = Time.ofEpochSec(t0).ymd();

		for (int i = 1; i < length; i++) {
			long t1 = ts[i];
			float price1 = prices_[i];
			String date1 = Time.ofEpochSec(t1).ymd();

			if (t1 <= t0)
				throw new RuntimeException("wrong date order: " + date0 + "/" + date1);

			if (price1 < 0f)
				throw new RuntimeException("price is negative: " + price1 + "/" + date1);

			if (Trade_.max < price1)
				throw new RuntimeException("price too high: " + price1 + "/" + date1);

			if (!Float.isFinite(price1))
				throw new RuntimeException("price is not finite: " + price1 + "/" + date1);

			if (!cleanse.isValid(price0, price1))
				throw new RuntimeException("price varied too much: " + price0 + "/" + date0 + " => " + price1 + "/" + date1);

			t0 = t1;
			price0 = price1;
			date0 = date1;
		}
	}

	private DataSource range_(TimeRange period) {
		long t0 = period.from.epochSec();
		long tx = period.to.epochSec();
		return range_(t0, tx);
	}

	private DataSource range_(long t0, long tx) {
		LongsBuilder ts1 = new LongsBuilder();
		List<Datum> data1 = new ArrayList<>();

		for (int i = 0; i < prices.length; i++) {
			Datum datum = getDatum_(i);
			long t = ts[i];
			if (t0 <= t && t < tx) {
				ts1.append(t);
				data1.add(datum);
			}
		}

		return of(ts1.toLongs().toArray(), Read.from(data1));
	}

	private LngFltPair get_(int pos) {
		if (pos < 0)
			pos += prices.length;
		return LngFltPair.of(ts[pos], prices[pos]);
	}

	private Datum getDatum(int start, int end) {
		if (start <= end)
			if (end <= 0)
				return Datum.of(Trade_.max);
			else if (ts.length <= start) // assume liquidated
				return Datum.of(Trade_.negligible);
			else if (start < end)
				return getDatum_(start, end);
			else
				return getDatum_(start);
		else
			throw new RuntimeException();
	}

	private Datum getDatum_(int start, int end) {
		float lo = Trade_.max;
		float hi = Trade_.negligible;
		for (int i = start; i < end; i++) {
			lo = Math.min(lo, lows[i]);
			hi = Math.min(hi, highs[i]);
		}
		return new Datum(opens[start], closes[end - 1], lo, hi);
	}

	private Datum getDatum_(int pos) { // instantaneous
		return Datum.of(opens[pos]);
	}

}
