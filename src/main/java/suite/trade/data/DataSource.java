package suite.trade.data;

import java.io.IOException;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collection;

import suite.math.linalg.Matrix;
import suite.streamlet.Read;
import suite.streamlet.Streamlet;
import suite.trade.DatePeriod;
import suite.util.DataInput_;
import suite.util.DataOutput_;
import suite.util.Object_;
import suite.util.Serialize;
import suite.util.Serialize.Serializer;
import suite.util.Set_;
import suite.util.To;
import suite.util.Util;

public class DataSource {

	public static Matrix mtx = new Matrix();

	public static Serializer<DataSource> serializer = new Serializer<DataSource>() {
		private Serializer<String[]> sas = Serialize.array(String.class, Serialize.string(10));
		private Serializer<float[]> fas = Serialize.arrayOfFloats;

		public DataSource read(DataInput_ dataInput) throws IOException {
			String[] dates = sas.read(dataInput);
			float[] prices = fas.read(dataInput);
			return new DataSource(dates, prices);
		}

		public void write(DataOutput_ dataOutput, DataSource dataSource) throws IOException {
			sas.write(dataOutput, dataSource.dates);
			fas.write(dataOutput, dataSource.prices);
		}
	};

	public final String[] dates;
	public final float[] prices;

	public class Datum {
		public final String date;
		public final float price;

		private Datum(String date, float price) {
			this.date = date;
			this.price = price;
		}

		public String toString() {
			return date + ":" + To.string(price);
		}
	}

	public interface DatePricePredicate {
		public boolean test(String date, float price);
	}

	public static AlignDataSource alignAll(Streamlet<DataSource> dataSources) {
		Streamlet<String> tradeDates;
		if (Boolean.TRUE)
			tradeDates = dataSources // union
					.concatMap(dataSource -> Read.from(dataSource.dates)) //
					.distinct();
		else
			tradeDates = Read.from(Set_.intersect(dataSources // intersect
					.map(dataSource -> (Collection<String>) Arrays.asList(dataSource.dates)) //
					.toList()));
		return new AlignDataSource(tradeDates.sort(Object_::compare).toArray(String.class));
	}

	public static class AlignDataSource {
		public final String[] dates;

		private AlignDataSource(String[] dates) {
			this.dates = dates;
		}

		public DataSource align(DataSource dataSource) {
			return dataSource.align(dates);
		}
	}

	public DataSource(String[] dates, float[] prices) {
		this.dates = dates;
		this.prices = prices;
		if (dates.length != prices.length)
			throw new RuntimeException("mismatched dates and prices");
	}

	public DataSource after(LocalDate date) {
		return range_(DatePeriod.of(date, DatePeriod.ages().to));
	}

	public DataSource align(String[] dates1) {
		int length0 = dates.length;
		int length1 = dates1.length;
		float[] prices1 = new float[length1];
		int si = 0;
		for (int di = 0; di < length1; di++) {
			String date = dates1[di];
			while (si < length0 && dates[si].compareTo(date) < 0)
				si++;
			prices1[di] = prices[si];
		}
		return new DataSource(dates1, prices1);
	}

	public DataSource cons(String date, float price) {
		String[] dates1 = Util.add(String.class, dates, new String[] { date, });
		float[] prices1 = mtx.concat(prices, new float[] { price, });
		return new DataSource(dates1, prices1);
	}

	public void cleanse() {

		// ignore price sparks caused by data source bugs
		for (int i = 2; i < prices.length; i++) {
			float price0 = prices[i - 2];
			float price1 = prices[i - 1];
			float price2 = prices[i - 0];
			if (isValid(price0, price2) && !isValid(price0, price1) && !isValid(price1, price2))
				prices[i - 1] = price0;
		}
	}

	public DataSource filter(DatePricePredicate fdp) {
		return filter_(fdp);
	}

	public Datum first() {
		return get(0);
	}

	public Datum get(int pos) {
		return get_(pos);
	}

	public Datum last() {
		return get(-1);
	}

	public DatePeriod period() {
		if (0 < dates.length) {
			LocalDate date0 = To.date(get(0).date);
			LocalDate datex = To.date(get(-1).date);
			return DatePeriod.of(date0, datex);
		} else
			throw new RuntimeException();
	}

	public DataSource range(DatePeriod period) {
		return range_(period);
	}

	public DataSource range(String s0, String sx) {
		return range_(s0, sx);
	}

	public void validate() {
		int length = prices.length;
		String date0 = dates[0];
		float price0 = prices[0];

		for (int i = 1; i < length; i++) {
			String date1 = dates[i];
			float price1 = prices[i];

			if (0 <= date0.compareTo(date1))
				throw new RuntimeException("wrong date order: " + date0 + "/" + date1);

			if (price1 <= 0f)
				throw new RuntimeException("price is zero or negative: " + price1 + "/" + date1);

			if (!Float.isFinite(price1))
				throw new RuntimeException("price is not finite: " + price1 + "/" + date1);

			if (!isValid(price0, price1))
				throw new RuntimeException("price varied too much: " + price0 + "/" + date0 + " => " + price1 + "/" + date1);

			date0 = date1;
			price0 = price1;
		}
	}

	@Override
	public String toString() {
		String range = 0 < prices.length ? first() + "~" + last() : "";
		return getClass().getSimpleName() + "(" + range + ")";
	}

	private DataSource range_(DatePeriod period) {
		String s0 = To.string(period.from);
		String sx = To.string(period.to);
		return range_(s0, sx);
	}

	private DataSource range_(String s0, String sx) {
		return filter_((date, price) -> Object_.compare(s0, date) <= 0 && Object_.compare(date, sx) < 0);
	}

	private DataSource filter_(DatePricePredicate fdp) {
		String[] dates1 = new String[dates.length];
		float[] prices1 = new float[prices.length];
		int j = 0;

		for (int i = 0; i < prices.length; i++) {
			String date = dates[i];
			float price = prices[i];
			if (fdp.test(date, price)) {
				dates1[j] = date;
				prices1[j] = price;
				j++;
			}
		}

		return new DataSource(Arrays.copyOf(dates1, j), Arrays.copyOf(prices1, j));
	}

	private Datum get_(int pos) {
		if (pos < 0)
			pos += prices.length;
		return new Datum(dates[pos], prices[pos]);
	}

	private boolean isValid(float price0, float price1) {
		float ratio = price1 / price0;
		return 1f / 2f < ratio && ratio < 2f / 1f;
	}

}
