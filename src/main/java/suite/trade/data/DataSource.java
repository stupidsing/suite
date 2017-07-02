package suite.trade.data;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;

import suite.math.linalg.Matrix;
import suite.primitive.LngFltPredicate;
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
import suite.util.DataInput_;
import suite.util.DataOutput_;
import suite.util.Object_;
import suite.util.Serialize;
import suite.util.Serialize.Serializer;
import suite.util.Set_;

public class DataSource {

	private Cleanse cleanse = new Cleanse();
	public static Matrix mtx = new Matrix();

	public static Serializer<DataSource> serializer = new Serializer<DataSource>() {
		private Serializer<String[]> sas = Serialize.array(String.class, Serialize.string(10));
		private Serializer<float[]> fas = Serialize.arrayOfFloats;

		public DataSource read(DataInput_ dataInput) throws IOException {
			long[] ts = Read //
					.from(sas.read(dataInput)) //
					.collect(LngStreamlet.of(s -> Time.of(s).epochSec())) //
					.toArray();
			float[] prices = fas.read(dataInput);
			return new DataSource(ts, prices);
		}

		public void write(DataOutput_ dataOutput, DataSource ds) throws IOException {
			String[] ymds = LngStreamlet //
					.of(ds.ts) //
					.map(ep -> Time.ofEpochSec(ep).ymd()) //
					.toArray(String.class);
			sas.write(dataOutput, ymds);
			fas.write(dataOutput, ds.prices);
		}
	};

	public final long[] ts;
	public final float[] prices;

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
			return ds.align(ts);
		}
	}

	public DataSource(long[] ts, float[] prices) {
		this.ts = ts;
		this.prices = prices;
		if (ts.length != prices.length)
			throw new RuntimeException("mismatched dates and prices");
	}

	public DataSource after(Time time) {
		return range_(TimeRange.of(time, TimeRange.max));
	}

	public DataSource align(long[] ts1) {
		int length0 = ts.length;
		int length1 = ts1.length;
		float[] prices1 = new float[length1];
		int si = 0, di = 0;
		while (di < length1)
			if (length0 <= si)
				prices1[di++] = Trade_.negligible; // avoid division by 0s
			else if (ts1[di] <= ts[si])
				prices1[di++] = prices[si];
			else
				si++;
		return new DataSource(ts1, prices1);
	}

	public DataSource cons(long time, float price) {
		int length = ts.length;
		long[] ts1 = Arrays.copyOf(ts, length + 1);
		float[] prices1 = mtx.concat(prices, new float[] { price, });
		ts1[length] = time;
		return new DataSource(ts1, prices1);
	}

	public DataSource filter(LngFltPredicate fdp) {
		return filter_(fdp);
	}

	public LngFltPair first() {
		return get(0);
	}

	public LngFltPair get(int pos) {
		return get_(pos);
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

	public TimeRange period() {
		if (0 < ts.length) {
			Time time0 = Time.ofEpochSec(get(0).t0);
			Time timex = Time.ofEpochSec(get(-1).t0);
			return TimeRange.of(time0, timex);
		} else
			throw new RuntimeException();
	}

	public DataSource range(TimeRange period) {
		return range_(period);
	}

	public DataSource range(long t0, long tx) {
		return range_(t0, tx);
	}

	public void validate() {
		int length = prices.length;
		long t0 = 0 < length ? ts[0] : Long.MIN_VALUE;
		float price0 = 0 < length ? prices[0] : Float.MAX_VALUE;

		for (int i = 1; i < length; i++) {
			long t1 = ts[i];
			float price1 = prices[i];
			String date0 = Time.ofEpochSec(t0).ymd();
			String date1 = Time.ofEpochSec(t1).ymd();

			if (t1 <= t0)
				throw new RuntimeException("wrong date order: " + date0 + "/" + date1);

			if (price1 < 0f)
				throw new RuntimeException("price is negative: " + price1 + "/" + date1);

			if (1E6f < price1)
				throw new RuntimeException("price too high: " + price1 + "/" + date1);

			if (!Float.isFinite(price1))
				throw new RuntimeException("price is not finite: " + price1 + "/" + date1);

			if (!cleanse.isValid(price0, price1))
				throw new RuntimeException("price varied too much: " + price0 + "/" + date0 + " => " + price1 + "/" + date1);

			t0 = t1;
			price0 = price1;
		}
	}

	@Override
	public String toString() {
		String range = 0 < prices.length ? first() + "~" + last() : "";
		return getClass().getSimpleName() + "(" + range + ")";
	}

	private DataSource range_(TimeRange period) {
		long t0 = period.from.epochSec();
		long tx = period.to.epochSec();
		return range_(t0, tx);
	}

	private DataSource range_(long t0, long sx) {
		return filter_((time, price) -> t0 <= time && time < sx);
	}

	private DataSource filter_(LngFltPredicate fdp) {
		long[] ts1 = new long[ts.length];
		float[] prices1 = new float[prices.length];
		int j = 0;

		for (int i = 0; i < prices.length; i++) {
			long t = ts[i];
			float price = prices[i];
			if (fdp.test(t, price)) {
				ts1[j] = t;
				prices1[j] = price;
				j++;
			}
		}

		return new DataSource(Arrays.copyOf(ts1, j), Arrays.copyOf(prices1, j));
	}

	private LngFltPair get_(int pos) {
		if (pos < 0)
			pos += prices.length;
		return LngFltPair.of(ts[pos], prices[pos]);
	}

}
