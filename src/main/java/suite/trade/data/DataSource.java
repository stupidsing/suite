package suite.trade.data;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;

import suite.math.linalg.Matrix;
import suite.primitive.LngFltPredicate;
import suite.primitive.LngPrimitives.Obj_Lng;
import suite.primitive.adt.pair.LngFltPair;
import suite.primitive.streamlet.LngStreamlet;
import suite.streamlet.Read;
import suite.streamlet.Streamlet;
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
			long[] dates = Read //
					.from(sas.read(dataInput)) //
					.collect(LngStreamlet.of(s -> Time.of(s).epochUtcSecond())) //
					.toArray();
			float[] prices = fas.read(dataInput);
			return new DataSource(dates, prices);
		}

		public void write(DataOutput_ dataOutput, DataSource ds) throws IOException {
			String[] dates = LngStreamlet //
					.of(ds.dates) //
					.map(ep -> Time.ofEpochUtcSecond(ep).ymd()) //
					.toArray(String.class);
			sas.write(dataOutput, dates);
			fas.write(dataOutput, ds.prices);
		}
	};

	public final long[] dates;
	public final float[] prices;

	public static AlignDataSource alignAll(Streamlet<DataSource> dataSources) {
		Streamlet<Long> tradeDates;
		if (Boolean.TRUE)
			tradeDates = dataSources // union
					.concatMap(ds -> LngStreamlet.of(ds.dates).map(date -> date)) //
					.distinct();
		else
			tradeDates = Read.from(Set_.intersect(dataSources // intersect
					.<Collection<Long>> map(ds -> LngStreamlet.of(ds.dates).map(date -> date).toList()) //
					.toList()));
		return new AlignDataSource(tradeDates //
				.sort(Object_::compare) //
				.collect(Obj_Lng.lift(date -> date)) //
				.toArray());
	}

	public static class AlignDataSource {
		public final long[] dates;

		private AlignDataSource(long[] dates) {
			this.dates = dates;
		}

		public DataSource align(DataSource ds) {
			return ds.align(dates);
		}
	}

	public DataSource(long[] dates, float[] prices) {
		this.dates = dates;
		this.prices = prices;
		if (dates.length != prices.length)
			throw new RuntimeException("mismatched dates and prices");
	}

	public DataSource after(Time time) {
		return range_(TimeRange.of(time, TimeRange.max));
	}

	public DataSource align(long[] dates1) {
		int length0 = dates.length;
		int length1 = dates1.length;
		float[] prices1 = new float[length1];
		int si = 0, di = 0;
		while (di < length1)
			if (length0 <= si)
				prices1[di++] = Trade_.negligible; // avoid division by 0s
			else if (dates1[di] <= dates[si])
				prices1[di++] = prices[si];
			else
				si++;
		return new DataSource(dates1, prices1);
	}

	public DataSource cons(long date, float price) {
		int length = dates.length;
		long[] dates1 = Arrays.copyOf(dates, length + 1);
		float[] prices1 = mtx.concat(prices, new float[] { price, });
		dates1[length] = date;
		return new DataSource(dates1, prices1);
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

	public TimeRange period() {
		if (0 < dates.length) {
			Time date0 = Time.ofEpochUtcSecond(get(0).t0);
			Time datex = Time.ofEpochUtcSecond(get(-1).t0);
			return TimeRange.of(date0, datex);
		} else
			throw new RuntimeException();
	}

	public DataSource range(TimeRange period) {
		return range_(period);
	}

	public DataSource range(long ep0, long epx) {
		return range_(ep0, epx);
	}

	public void validate() {
		int length = prices.length;
		long date0 = 0 < length ? dates[0] : Long.MIN_VALUE;
		float price0 = 0 < length ? prices[0] : Float.MAX_VALUE;

		for (int i = 1; i < length; i++) {
			long date1 = dates[i];
			float price1 = prices[i];
			String ymd0 = Time.ofEpochUtcSecond(date0).ymd();
			String ymd1 = Time.ofEpochUtcSecond(date1).ymd();

			if (date1 <= date0)
				throw new RuntimeException("wrong date order: " + ymd0 + "/" + ymd1);

			if (price1 < 0f)
				throw new RuntimeException("price is negative: " + price1 + "/" + ymd1);

			if (999f < price1)
				throw new RuntimeException("price too high: " + price1 + "/" + ymd1);

			if (!Float.isFinite(price1))
				throw new RuntimeException("price is not finite: " + price1 + "/" + ymd1);

			if (!cleanse.isValid(price0, price1))
				throw new RuntimeException("price varied too much: " + price0 + "/" + ymd0 + " => " + price1 + "/" + ymd1);

			date0 = date1;
			price0 = price1;
		}
	}

	@Override
	public String toString() {
		String range = 0 < prices.length ? first() + "~" + last() : "";
		return getClass().getSimpleName() + "(" + range + ")";
	}

	private DataSource range_(TimeRange period) {
		long ep0 = period.from.epochUtcSecond();
		long epx = period.to.epochUtcSecond();
		return range_(ep0, epx);
	}

	private DataSource range_(long s0, long sx) {
		return filter_((date, price) -> s0 <= date && date < sx);
	}

	private DataSource filter_(LngFltPredicate fdp) {
		long[] dates1 = new long[dates.length];
		float[] prices1 = new float[prices.length];
		int j = 0;

		for (int i = 0; i < prices.length; i++) {
			long date = dates[i];
			float price = prices[i];
			if (fdp.test(date, price)) {
				dates1[j] = date;
				prices1[j] = price;
				j++;
			}
		}

		return new DataSource(Arrays.copyOf(dates1, j), Arrays.copyOf(prices1, j));
	}

	private LngFltPair get_(int pos) {
		if (pos < 0)
			pos += prices.length;
		return LngFltPair.of(dates[pos], prices[pos]);
	}

}
