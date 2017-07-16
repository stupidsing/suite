package suite.trade.data;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import suite.math.stat.TimeSeries;
import suite.primitive.Floats_;
import suite.primitive.FltPrimitives.Obj_Flt;
import suite.primitive.LngPrimitives.Obj_Lng;
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

	public static long tickDuration = 7l * 3600l; // 09:30 to 16:30

	private static Cleanse cleanse = new Cleanse();
	private static TimeSeries timeSeries = new TimeSeries();

	public final long[] ts;
	public final float[] prices;
	public final float[] opens, closes;
	public final float[] lows, highs;
	public final float[] volumes;

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

	public static DataSource of(Streamlet<Datum> data) {
		return of(data.collect(Obj_Lng.lift(datum -> datum.t0)).toArray(), data);
	}

	private static DataSource of(long[] ts, Streamlet<Datum> data) {
		return ofOhlcv( //
				ts, //
				data.collect(Obj_Flt.lift(datum -> datum.open)).toArray(), //
				data.collect(Obj_Flt.lift(datum -> datum.close)).toArray(), //
				data.collect(Obj_Flt.lift(datum -> datum.low)).toArray(), //
				data.collect(Obj_Flt.lift(datum -> datum.high)).toArray(), //
				data.collect(Obj_Flt.lift(datum -> datum.volume)).toArray());
	}

	public static DataSource of(long[] ts, float[] prices) {
		return ofOhlcv(ts, prices, prices, prices, prices, new float[ts.length]);
	}

	// at the end of the day -
	// current price = today's closing price;
	// next price = tomorrow's opening price.
	public static DataSource ofOhlcv(long[] ts, float[] opens, float[] closes, float[] lows, float[] highs, float[] volumes) {
		return new DataSource(ts, opens, closes, lows, highs, volumes);
	}

	private DataSource(long[] ts, float[] opens, float[] closes, float[] lows, float[] highs, float[] volumes) {
		super();
		this.ts = ts;
		this.prices = closes;
		this.opens = opens;
		this.closes = closes;
		this.lows = lows;
		this.highs = highs;
		this.volumes = volumes;
		if (ts.length != prices.length //
				|| ts.length != opens.length || ts.length != closes.length //
				|| ts.length != lows.length || ts.length != highs.length //
				|| ts.length != volumes.length)
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
			data[di] = tickDatum(si + 1, si_ + 1);
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
			data[di] = tickDatum(si_, si);
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
		return ofOhlcv(ts1, //
				Floats_.concat(opens, new float[] { price, }), //
				Floats_.concat(closes, new float[] { price, }), //
				Floats_.concat(lows, new float[] { price, }), //
				Floats_.concat(highs, new float[] { price, }), //
				Floats_.concat(volumes, new float[] { 0f, }));
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
			sb.append(prefix + "[" + Time.ofEpochSec(ts[i]) + "]" //
					+ " o/c:" + To.string(opens[i]) + "/" + To.string(closes[i]) //
					+ " l/h:" + To.string(lows[i]) + "/" + To.string(highs[i]) //
					+ " v:" + To.string(volumes[i]) //
					+ "\n");
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
		List<Datum> data1 = new ArrayList<>();

		for (int i = 0; i < prices.length; i++) {
			Datum datum = datum_(i);
			if (t0 <= datum.t0 && datum.tx <= tx)
				data1.add(datum);
		}

		return of(Read.from(data1));
	}

	private LngFltPair get_(int pos) {
		if (pos < 0)
			pos += prices.length;
		return LngFltPair.of(ts[pos], prices[pos]);
	}

	private Datum tickDatum(int start, int end) {
		if (start <= end)
			if (end <= 0) {
				long t = TimeRange.min.epochSec();
				return Datum.of(t, t, Trade_.max);
			} else if (ts.length <= start) {
				long t = TimeRange.max.epochSec();
				return Datum.of(t, t, Trade_.negligible);
			} else if (start < end)
				return datum_(start, end);
			else
				return instant(start);
		else
			throw new RuntimeException();
	}

	private Datum datum_(int start, int end) {
		float lo = Trade_.max;
		float hi = Trade_.negligible;
		float volume = 0f;
		for (int i = start; i < end; i++) {
			lo = Math.min(lo, lows[i]);
			hi = Math.max(hi, highs[i]);
			volume += volumes[i];
		}
		return new Datum(ts[start], ts[end - 1] + tickDuration, opens[start], closes[end - 1], lo, hi, volume);
	}

	private Datum datum_(int pos) {
		long t0 = ts[pos];
		return new Datum(t0, t0 + tickDuration, opens[pos], closes[pos], lows[pos], highs[pos], volumes[pos]);
	}

	private Datum instant(int pos) {
		long t0 = ts[pos];
		return Datum.of(t0, t0, opens[pos]);
	}

	public static class Datum {
		public final long t0, tx;
		public final float open, close;
		public final float low, high;
		public final float volume;

		private static Datum of(long t0, long tx, float price) {
			return new Datum(t0, tx, price, price, price, price, 0f);
		}

		public Datum(long t0, long tx, float open, float close, float low, float high, float volume) {
			this.t0 = t0;
			this.tx = tx;
			this.open = open;
			this.close = close;
			this.low = low;
			this.high = high;
			this.volume = volume;
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

}
