package suite.trade.data;

import static java.lang.Math.max;
import static java.lang.Math.min;
import static primal.statics.Fail.fail;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import primal.Verbs.Compare;
import primal.primitive.adt.pair.LngFltPair;
import suite.primitive.AsFlt;
import suite.primitive.AsLng;
import suite.primitive.Longs_;
import suite.streamlet.Read;
import suite.streamlet.Streamlet;
import suite.streamlet.Streamlet2;
import suite.trade.Time;
import suite.trade.TimeRange;
import suite.trade.Trade_;
import suite.ts.TimeSeries;
import suite.util.Set_;
import suite.util.To;

// all prices should be already adjusted according to corporate service actions
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
		var ts = alignAll(dsByKey0.values());

		return dsByKey0 //
				.mapValue(ds -> ds.alignBeforePrices(ts)) //
				.collect() //
				.apply(st -> new AlignKeyDataSource<>(ts, st));
	}

	public static class AlignKeyDataSource<K> {
		public final long[] ts;
		public final Streamlet2<K, DataSource> dsByKey;

		public AlignKeyDataSource(long[] ts, Streamlet2<K, DataSource> dsByKey) {
			this.ts = ts;
			this.dsByKey = dsByKey;
		}

		public AlignKeyDataSource<K> trim(int end) {
			return new AlignKeyDataSource<>(Arrays.copyOf(ts, end), dsByKey.mapValue(ds -> ds.trim(end)).collect());
		}
	}

	public static long[] alignAll(Streamlet<DataSource> dataSources) {
		Streamlet<Long> tradeTimes;
		if (Boolean.TRUE)
			tradeTimes = dataSources // union
					.concatMap(ds -> Longs_.of(ds.ts).map(t -> t)) //
					.distinct();
		else
			tradeTimes = Read.from(Set_.intersect(dataSources // intersect
					.<Collection<Long>> map(ds -> Longs_.of(ds.ts).map(t -> t).toList()) //
					.toList()));
		return tradeTimes.sort(Compare::objects).collect(AsLng.lift(t -> t)).toArray();
	}

	public static DataSource of(Streamlet<Datum> data) {
		return of(data.collect(AsLng.lift(datum -> datum.t0)).toArray(), data);
	}

	private static DataSource of(long[] ts, Streamlet<Datum> data) {
		return ofOhlcv( //
				ts, //
				data.collect(AsFlt.lift(datum -> datum.open)).toArray(), //
				data.collect(AsFlt.lift(datum -> datum.close)).toArray(), //
				data.collect(AsFlt.lift(datum -> datum.low)).toArray(), //
				data.collect(AsFlt.lift(datum -> datum.high)).toArray(), //
				data.collect(AsFlt.lift(datum -> datum.volume)).toArray());
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
			fail("mismatched dates and prices");
	}

	public DataSource after(Time time) {
		return range_(TimeRange.of(time, TimeRange.max));
	}

	public DataSource alignAfterPrices(long[] ts1) {
		var length0 = ts.length;
		var length1 = ts1.length;
		var data = new Datum[length1];
		var si = length0 - 1;

		for (var di = length1 - 1; 0 <= di; di--) {
			var si_ = si;
			var t1 = ts1[di];
			while (0 <= si && t1 <= ts[si])
				si--;
			data[di] = tickDatum(si + 1, si_ + 1);
		}

		return of(ts1, Read.from(data));
	}

	public DataSource alignBeforePrices(long[] ts1) {
		var length0 = ts.length;
		var length1 = ts1.length;
		var data = new Datum[length1];
		var si = 0;

		for (var di = 0; di < length1; di++) {
			var si_ = si;
			var t1 = ts1[di];
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

	public LngFltPair first() {
		return get(0);
	}

	public LngFltPair get(int pos) {
		return get_(pos);
	}

	public Eod getEod(int pos) {
		var pos1 = pos + 1;
		if (pos1 < ts.length)
			return new Eod(prices[pos], opens[pos1], lows[pos1], highs[pos1]);
		else
			return Eod.of(prices[pos]);
	}

	public LngFltPair last() {
		return get(-1);
	}

	public LngFltPair last(Time time) {
		var t = time.epochSec();
		for (var i = ts.length - 1; 0 <= i; i--)
			if (ts[i] <= t)
				return LngFltPair.of(ts[i], prices[i]);
		return null;
	}

	public double lastReturn(int index) {
		var price0 = prices[index - 2];
		var price1 = prices[index - 1];
		return (price1 - price0) / price0;
	}

	public DataSource range(TimeRange period) {
		return range_(period);
	}

	public DataSource range(long t0, long tx) {
		return range_(t0, tx);
	}

	public String recent(String prefix, int size) {
		return To.string(sb -> {
			for (var i = ts.length - size; i < ts.length; i++)
				sb.append(prefix + "[" + Time.ofEpochSec(ts[i]) + "]" //
						+ " o/c:" + To.string(opens[i]) + "/" + To.string(closes[i]) //
						+ " l/h:" + To.string(lows[i]) + "/" + To.string(highs[i]) //
						+ " v:" + To.string(volumes[i]) //
						+ "\n");
		});
	}

	public float[] returns() {
		return timeSeries.returns(prices);
	}

	public DataSource trim(int end) {
		return ofOhlcv( //
				Arrays.copyOf(ts, end), //
				Arrays.copyOf(opens, end), //
				Arrays.copyOf(closes, end), //
				Arrays.copyOf(lows, end), //
				Arrays.copyOf(highs, end), //
				Arrays.copyOf(volumes, end));
	}

	public DataSource validate() {
		validate(prices);
		for (var prices1 : List.of(opens, closes, lows, highs))
			if (prices != prices1)
				validate(prices1);
		return this;
	}

	@Override
	public String toString() {
		var range = 0 < prices.length ? first() + "~" + last() : "";
		return getClass().getSimpleName() + "(" + range + ")";
	}

	private void validate(float[] prices_) {
		var length = prices_.length;
		var t0 = 0 < length ? ts[0] : Long.MIN_VALUE;
		var price0 = 0 < length ? prices_[0] : Float.MAX_VALUE;
		var date0 = Time.ofEpochSec(t0).ymd();

		for (var i = 1; i < length; i++) {
			var t1 = ts[i];
			var price1 = prices_[i];
			var date1 = Time.ofEpochSec(t1).ymd();

			if (t1 <= t0)
				fail("wrong date order: " + date0 + "/" + date1);

			if (price1 < 0f)
				fail("price is negative: " + price1 + "/" + date1);

			if (Trade_.max < price1)
				fail("price too high: " + price1 + "/" + date1);

			if (!Float.isFinite(price1))
				fail("price is not finite: " + price1 + "/" + date1);

			if (!cleanse.isValid(price0, price1))
				fail("price varied too much: " + price0 + "/" + date0 + " => " + price1 + "/" + date1);

			t0 = t1;
			price0 = price1;
			date0 = date1;
		}
	}

	private DataSource range_(TimeRange period) {
		var t0 = period.fr.epochSec();
		var tx = period.to.epochSec();
		return range_(t0, tx);
	}

	private DataSource range_(long t0, long tx) {
		var data1 = new ArrayList<Datum>();

		for (var i = 0; i < prices.length; i++) {
			var datum = datum_(i);
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
				var t = TimeRange.min.epochSec();
				return Datum.of(t, t, Trade_.max);
			} else if (ts.length <= start) {
				var t = TimeRange.max.epochSec();
				return Datum.of(t, t, Trade_.negligible);
			} else if (start < end)
				return datum_(start, end);
			else
				return instant(start);
		else
			return fail();
	}

	private Datum datum_(int start, int end) {
		var lo = Trade_.max;
		var hi = Trade_.negligible;
		var volume = 0f;
		for (var i = start; i < end; i++) {
			lo = min(lo, lows[i]);
			hi = max(hi, highs[i]);
			volume += volumes[i];
		}
		return new Datum(ts[start], ts[end - 1] + tickDuration, opens[start], closes[end - 1], lo, hi, volume);
	}

	private Datum datum_(int pos) {
		var t0 = ts[pos];
		return new Datum(t0, t0 + tickDuration, opens[pos], closes[pos], lows[pos], highs[pos], volumes[pos]);
	}

	private Datum instant(int pos) {
		var t0 = ts[pos];
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
