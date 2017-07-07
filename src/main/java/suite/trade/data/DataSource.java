package suite.trade.data;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import suite.math.linalg.Matrix;
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

	private Cleanse cleanse = new Cleanse();
	public static Matrix mtx = new Matrix();

	public final long[] ts;
	public final float[] prices;

	// prices of next tick, e.g. tomorrow's open
	public final float[] nextOpens;

	public static class Eod {
		public final float price;
		public final float nextOpen;

		public static Eod of(float price) {
			return of(price, price);
		}

		public static Eod of(float price, float nextOpen) {
			return new Eod(price, nextOpen);
		}

		private Eod(float price, float nextOpen) {
			this.price = price;
			this.nextOpen = nextOpen;
		}
	}

	public static <K> AlignKeyDataSource<K> alignAll(Streamlet2<K, DataSource> dsByKey0) {
		AlignDataSource alignDataSource = DataSource.alignAll(dsByKey0.values());

		return dsByKey0 //
				.mapValue(alignDataSource::align) //
				.collect(As::streamlet2) //
				.apply(st -> new AlignKeyDataSource<>(alignDataSource, st));
	}

	public static class AlignKeyDataSource<K> {
		public final long[] ts;
		public final Streamlet2<K, DataSource> dsByKey;

		private AlignKeyDataSource(AlignDataSource alignDataSource, Streamlet2<K, DataSource> dsByKey) {
			this.ts = alignDataSource.ts;
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
			return ds.alignAfterPrices(ts);
		}
	}

	// at the end of the day -
	// current price = today's closing price;
	// next price = tomorrow's opening price.
	public static DataSource ofOpenClose(long[] ts, float[] opens, float[] closes) {
		int length = opens.length;
		int lengthm1 = length - 1;
		float[] nexts = new float[length];
		Floats_.copy(opens, 1, nexts, 0, lengthm1);
		nexts[lengthm1] = closes[lengthm1];
		return of(ts, closes, nexts);
	}

	public static DataSource of(long[] ts, float[] prices) {
		return of(ts, prices, prices);
	}

	public static DataSource of(long[] ts, Streamlet<Eod> pairs) {
		float[] prices = pairs.collect(Obj_Flt.lift(pair -> pair.price)).toArray();
		float[] nextOpens = pairs.collect(Obj_Flt.lift(pair -> pair.nextOpen)).toArray();
		return of(ts, prices, nextOpens);
	}

	public static DataSource of(long[] ts, float[] prices, float[] nextOpens) {
		return new DataSource(ts, prices, nextOpens);
	}

	private DataSource(long[] ts, float[] prices, float[] nextOpens) {
		this.ts = ts;
		this.prices = prices;
		this.nextOpens = nextOpens;
		if (ts.length != prices.length || ts.length != nextOpens.length)
			throw new RuntimeException("mismatched dates and prices");
	}

	public DataSource after(Time time) {
		return range_(TimeRange.of(time, TimeRange.max));
	}

	public DataSource alignAfterPrices(long[] ts1) {
		int length0 = ts.length;
		int length1 = ts1.length;
		Eod[] pairs1 = new Eod[length1];
		int si = 0, di = 0;
		while (di < length1)
			if (length0 <= si)
				// avoid division by 0s
				pairs1[di++] = Eod.of(Trade_.negligible);
			else if (ts1[di] <= ts[si])
				pairs1[di++] = getEod_(si);
			else
				si++;
		return of(ts1, Read.from(pairs1));
	}

	public DataSource alignBeforePrices(long[] ts1) {
		int length0 = ts.length;
		int length1 = ts1.length;
		Eod[] pairs1 = new Eod[length1];
		int si = length0 - 1, di = length1 - 1;
		while (0 <= di)
			if (si < 0)
				// avoid division by 0s
				pairs1[di--] = Eod.of(Trade_.max);
			else if (ts[si] <= ts1[di])
				pairs1[di--] = getEod_(si);
			else
				si--;
		return of(ts1, Read.from(pairs1));
	}

	public DataSource cons(long time, float price) {
		int length = ts.length;
		long[] ts1 = Arrays.copyOf(ts, length + 1);
		float[] prices1 = Floats_.concat(prices, new float[] { price, });
		float[] nextOpens1 = prices1 = Floats_.concat(nextOpens, new float[] { price, });
		ts1[length] = time;
		return of(ts1, prices1, nextOpens1);
	}

	public LngFltPair first() {
		return get(0);
	}

	public LngFltPair get(int pos) {
		return get_(pos);
	}

	public Eod getEod(int pos) {
		return getEod_(pos);
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

	public void validate() {
		validate(prices);
		if (prices != nextOpens)
			validate(nextOpens);
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
		List<Eod> pairs1 = new ArrayList<>();

		for (int i = 0; i < prices.length; i++) {
			long t = ts[i];
			Eod pair = getEod_(i);
			if (t0 <= t && t < tx) {
				ts1.append(t);
				pairs1.add(pair);
			}
		}

		return of(ts1.toLongs().toArray(), Read.from(pairs1));
	}

	private LngFltPair get_(int pos) {
		if (pos < 0)
			pos += prices.length;
		return LngFltPair.of(ts[pos], prices[pos]);
	}

	private Eod getEod_(int pos) {
		return Eod.of(prices[pos], nextOpens[pos]);
	}

}
