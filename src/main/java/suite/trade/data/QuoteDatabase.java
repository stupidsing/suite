package suite.trade.data;

import java.util.Iterator;
import java.util.Map;
import java.util.SortedSet;

import suite.streamlet.Read;
import suite.trade.data.TextDatabase.Datum;
import suite.util.HomeDir;
import suite.util.To;

public class QuoteDatabase {

	private TextDatabase textDatabase = new TextDatabase(HomeDir.resolve("quote-database.csv"));

	public DataSource get(String symbol, String field) {
		Datum datum0 = datum(symbol, field, "", 0f);
		Datum datumx = datum(symbol, field, "~", 0f);
		SortedSet<Datum> set = textDatabase.range(datum0, datumx);
		Iterator<Datum> iter = set.iterator();
		int size = set.size();
		String[] dates = new String[size];
		float[] prices = new float[size];

		for (int i = 0; i < size; i++) {
			Datum datum = iter.next();
			dates[i] = fromKey(datum.key)[2];
			prices[i] = Float.parseFloat(datum.value);
		}

		return new DataSource(dates, prices);
	}

	public void join() {
		textDatabase.join();
	}

	public void merge(String field, Map<String, DataSource> dataSourceBySymbol) {
		textDatabase.merge(Read.from2(dataSourceBySymbol) //
				.concatMap((symbol, dataSource) -> {
					String[] dates = dataSource.dates;
					float[] prices = dataSource.prices;
					int length = prices.length;
					return Read.range(length).map(i -> datum(symbol, field, dates[i], prices[i]));
				}));
	}

	private Datum datum(String symbol, String field, String date, float price) {
		return textDatabase.datum(toKey(symbol, field, date), To.string(price));
	}

	public String toKey(String symbol, String field, String date) {
		return symbol + "/" + field + "/" + date;
	}

	public String[] fromKey(String key) {
		return key.split("/");
	}

}
