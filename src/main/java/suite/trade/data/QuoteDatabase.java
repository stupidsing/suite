package suite.trade.data;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.Objects;
import java.util.SortedSet;
import java.util.TreeSet;

import suite.os.FileUtil;
import suite.primitive.Bytes;
import suite.primitive.BytesUtil;
import suite.streamlet.As;
import suite.streamlet.Outlet;
import suite.streamlet.Read;
import suite.util.HomeDir;
import suite.util.To;
import suite.util.Util;

public class QuoteDatabase {

	private Path path = HomeDir.resolve("quote-database.csv");
	private TreeSet<Datum> data = new TreeSet<>();

	public QuoteDatabase() {
		load();
	}

	public DataSource get(String stockCode, String field) {
		Datum datum0 = new Datum(stockCode, field, "", 0f);
		Datum datumx = new Datum(stockCode, field, "~", 0f);
		SortedSet<Datum> set = data.subSet(datum0, datumx);
		Iterator<Datum> iter = set.iterator();
		int size = set.size();
		String[] dates = new String[size];
		float[] prices = new float[size];

		for (int i = 0; i < size; i++) {
			Datum datum = iter.next();
			dates[i] = fromKey(datum.key)[2];
			prices[i] = datum.price;
		}

		return new DataSource(dates, prices);
	}

	public void merge(String stockCode, String field, DataSource dataSource) {
		String[] dates = dataSource.dates;
		float[] prices = dataSource.prices;
		int length = prices.length;
		for (int i = 0; i < length; i++)
			merge(new Datum(stockCode, field, dates[i], prices[i]));
		save();
	}

	private void load() {
		if (Files.exists(path))
			Read.bytes(path) //
					.collect(As::lines) //
					.map(this::toDatum) //
					.forEach(this::merge);
	}

	private void save() {
		Outlet<Bytes> outlet = Read.from(data) //
				.map(this::toBytes) //
				.collect(BytesUtil::buffer);

		try (OutputStream os = FileUtil.out(path)) {
			BytesUtil.copy(outlet, os);
		} catch (IOException ex) {
			throw new RuntimeException(ex);
		}
	}

	private void merge(Datum datum) {
		Datum datum0 = data.ceiling(datum);
		if (datum0 == null || !Objects.equals(datum, datum0))
			data.add(datum);
		else if (datum.price != datum0.price)
			throw new RuntimeException();
	}

	private Datum toDatum(Bytes bytes) {
		String[] array = To.string(bytes).split(",");
		return new Datum(array[0], Float.parseFloat(array[1]));
	}

	private Bytes toBytes(Datum datum) {
		return To.bytes(datum.key + "," + datum.price + "\n");
	}

	private class Datum implements Comparable<Datum> {
		private final String key;
		private final float price;

		private Datum(String stockCode, String field, String date, float price) {
			this(toKey(stockCode, field, date), price);
		}

		private Datum(String key, float price) {
			this.key = key;
			this.price = price;
		}

		public int compareTo(Datum other) {
			return Util.compare(key, other.key);
		}

		public boolean equals(Object object) {
			if (object.getClass() == Datum.class) {
				Datum other = (Datum) object;
				return Util.stringEquals(key, other.key);
			} else
				return false;
		}

		public int hashCode() {
			return Objects.hashCode(key);
		}
	}

	public String toKey(String stockCode, String field, String date) {
		return stockCode + "/" + field + "/" + date;
	}

	public String[] fromKey(String key) {
		return key.split("/");
	}

}
