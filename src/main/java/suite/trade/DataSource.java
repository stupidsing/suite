package suite.trade;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.time.LocalDate;
import java.util.Arrays;

import suite.math.Matrix;
import suite.util.FormatUtil;
import suite.util.Serialize;
import suite.util.Serialize.Serializer;
import suite.util.Util;

public class DataSource {

	public static Matrix mtx = new Matrix();

	public static Serializer<DataSource> serializer = new Serializer<DataSource>() {
		private Serializer<String[]> sas = Serialize.array(String.class, Serialize.string(10));
		private Serializer<float[]> fas = Serialize.floatArray;

		public DataSource read(DataInput dataInput) throws IOException {
			String[] dates = sas.read(dataInput);
			float[] prices = fas.read(dataInput);
			return new DataSource(dates, prices);
		}

		public void write(DataOutput dataOutput, DataSource dataSource) throws IOException {
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
	}

	public DataSource(String[] dates, float[] prices) {
		this.dates = dates;
		this.prices = prices;
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

	public DataSource limitAfter(LocalDate date) {
		return limit(DatePeriod.of(date, DatePeriod.ages().to));
	}

	public DataSource limit(DatePeriod period) {
		String s0 = FormatUtil.formatDate(period.from);
		String sx = FormatUtil.formatDate(period.to);
		String[] dates1 = new String[dates.length];
		float[] prices1 = new float[prices.length];
		int j = 0;

		for (int i = 0; i < prices.length; i++) {
			String date = dates[i];
			float price = prices[i];
			if (Util.compare(s0, date) <= 0 && Util.compare(date, sx) < 0) {
				dates1[j] = date;
				prices1[j] = price;
				j++;
			}
		}

		return new DataSource(Arrays.copyOf(dates1, j), Arrays.copyOf(prices1, j));
	}

	// at least approximately 2 years of data
	public void validateTwoYears() {
		if (2 * 240 <= prices.length)
			validate();
		else
			throw new RuntimeException("Not enough data");
	}

	public void validate() {
		int length = prices.length;
		String date0 = dates[0];
		float price0 = prices[0];

		for (int i = 1; i < length; i++) {
			String date1 = dates[i];
			float price1 = prices[i];

			if (0 <= date0.compareTo(date1))
				throw new RuntimeException("Wrong date order: " + date0 + "/" + date1);

			if (price1 == 0f)
				throw new RuntimeException("Price is zero: " + price1 + "/" + date1);

			if (!Float.isFinite(price1))
				throw new RuntimeException("Price is not finite: " + price1 + "/" + date1);

			if (!isValid(price0, price1))
				throw new RuntimeException("Price varied too much: " + price0 + "/" + date0 + " => " + price1 + "/" + date1);

			date0 = date1;
			price0 = price1;
		}
	}

	public float nYears() { // approximately
		LocalDate dateStart = FormatUtil.date(get(0).date);
		LocalDate dateEnd = FormatUtil.date(get(-1).date);
		return (dateEnd.toEpochDay() - dateStart.toEpochDay()) / 365f;
	}

	public Datum get(int pos) {
		if (pos < 0)
			pos += prices.length;
		return new Datum(dates[pos], prices[pos]);
	}

	private boolean isValid(float price0, float price1) {
		float ratio = price1 / price0;
		return 1f / 2f < ratio && ratio < 2f / 1f;
	}

}
