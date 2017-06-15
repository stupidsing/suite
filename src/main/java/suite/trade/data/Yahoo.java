package suite.trade.data;

import java.io.InputStream;
import java.net.URL;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiFunction;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import suite.Constants;
import suite.adt.pair.Pair;
import suite.http.HttpUtil;
import suite.node.util.Singleton;
import suite.streamlet.As;
import suite.streamlet.Read;
import suite.streamlet.Streamlet;
import suite.trade.Time;
import suite.trade.TimeRange;
import suite.trade.Trade_;
import suite.util.Object_;
import suite.util.Rethrow;
import suite.util.To;

public class Yahoo {

	private static ObjectMapper mapper = new ObjectMapper();

	public DataSource dataSourceCsv(String symbol, TimeRange period) {
		String urlString = tableUrl(symbol, period);

		// Date, Open, High, Low, Close, Volume, Adj Close
		List<String[]> arrays = Singleton.get() //
				.getStoreCache() //
				.http(urlString) //
				.collect(As::csv) //
				.skip(1) //
				.sort((a0, a1) -> Object_.compare(a0[0], a1[0])) //
				.toList();

		String[] dates = Read.from(arrays) //
				.map(array -> array[0]) //
				.toArray(String.class);

		float[] prices = Read.from(arrays) //
				.collect(As.arrayOfFloats(array -> Float.parseFloat(array[1])));

		adjust(symbol, dates, prices);

		DataSource dataSource = new DataSource(dates, prices);
		dataSource.cleanse();

		return dataSource;
	}

	public DataSource dataSourceL1(String symbol, TimeRange period) {
		JsonNode json = queryL1(symbol, period);

		Streamlet<JsonNode> jsons = Read.each(json) //
				.flatMap(json_ -> json_.get("chart").get("result"));

		String[] dates = jsons //
				.flatMap(json_ -> json_.get("timestamp")) //
				.map(json_ -> Time.ofEpochUtcSecond(json_.longValue())) //
				.map(Time::ymd) //
				.toArray(String.class);

		float[] prices = jsons //
				.flatMap(json_ -> json_.get("indicators").get("quote")) //
				.flatMap(json_ -> json_.get("open")) //
				.collect(As.arrayOfFloats(JsonNode::floatValue));

		return new DataSource(dates, prices).filter((date, price) -> price != 0f);
	}

	public DataSource dataSourceManuallyAdjustedCloseL1(String symbol, TimeRange period) {
		JsonNode json = queryL1(symbol, period);

		Streamlet<JsonNode> jsons = Read.each(json) //
				.flatMap(json_ -> json_.get("chart").get("result"));

		int[] epochs = jsons //
				.flatMap(json_ -> json_.get("timestamp")) //
				.collect(As.arrayOfInts(json_ -> json_.intValue()));

		float[] prices = jsons //
				.flatMap(json_ -> json_.get("unadjquote").get("unadjclose")) //
				.flatMap(json_ -> json_.get("open")) //
				.collect(As.arrayOfFloats(JsonNode::floatValue));

		int length = epochs.length;
		String[] dates = new String[length];

		List<Pair<Long, Float>> dividends = jsons //
				.flatMap(json_ -> json_.get("events").get("dividends")) //
				.map2(json_ -> json_.get("date").longValue(), json_ -> json_.get("amount").floatValue()) //
				.sortByKey((l0, l1) -> Long.compare(l1, l0)) //
				.toList();

		List<Pair<Long, Float>> splits = jsons //
				.flatMap(json_ -> json_.get("events").get("splits")) //
				.map2(json_ -> json_.get("date").longValue(),
						json_ -> json_.get("numerator").floatValue() / json_.get("denominator").floatValue()) //
				.sortByKey((l0, l1) -> Long.compare(l1, l0)) //
				.toList();

		for (int i = 0; i < length; i++)
			dates[i] = Time.ofEpochUtcSecond(epochs[i]).ymd();

		int di = dividends.size() - 1;
		int si = splits.size() - 1;
		float a = 0f, b = 1f;

		for (int i = length - 1; 0 <= i; i--) {
			prices[i] = a + b * prices[i];
			int epoch = epochs[i];

			if (0 <= di) {
				Pair<Long, Float> dividend = dividends.get(di);

				if (dividend.t0 == epoch) {
					a -= dividend.t1;
					di--;
				}
			}

			if (0 <= si) {
				Pair<Long, Float> split = splits.get(si);

				if (split.t0 == epoch) {
					a *= split.t1;
					b *= split.t1;
					si--;
				}
			}
		}

		return new DataSource(dates, prices);
	}

	private JsonNode queryL1(String symbol, TimeRange period) {
		String urlString = "https://l1-query.finance.yahoo.com/v7/finance/chart/" //
				+ encode(symbol) //
				+ "?period1=" + period.from.startOfDay().epochUtcSecond() //
				+ "&period2=" + period.to.startOfDay().epochUtcSecond() //
				+ "&interval=1d" //
				+ "&indicators=quote" //
				+ "&includeTimestamps=true" //
				+ "&includePrePost=true" //
				+ "&events=div%7Csplit%7Cearn" //
				+ "&corsDomain=finance.yahoo.com";

		JsonNode json = Rethrow.ex(() -> {
			try (InputStream is = Singleton.get().getStoreCache().http(urlString).collect(To::inputStream)) {
				return mapper.readTree(is);
			}
		});
		return json;
	}

	public DataSource dataSourceYql(String symbol, TimeRange period) {
		String yql = "select *" //
				+ " from yahoo.finance.historicaldata" //
				+ " where symbol = \"" + symbol + "\"" //
				+ " and startDate = \"" + period.from.ymd() + "\"" //
				+ " and endDate = \"" + period.to.ymd() + "\"";

		String urlString = "http://query.yahooapis.com/v1/public/yql" //
				+ "?q=" + encode(yql) //
				+ "&format=json" //
				+ "&diagnostics=true" //
				+ "&env=store%3A%2F%2Fdatatables.org%2Falltableswithkeys" //
				+ "&callback=";

		return Rethrow.ex(() -> {
			try (InputStream is = Singleton.get().getStoreCache().http(urlString).collect(To::inputStream)) {
				JsonNode json = mapper.readTree(is);

				Streamlet<String[]> arrays = Read.each(json) //
						.flatMap(json_ -> json_.get("query")) //
						.flatMap(json_ -> json_.get("results")) //
						.flatMap(json_ -> json_.get("quote")) //
						.map(json_ -> new String[] { json_.get("Date").textValue(), json_.get("Open").textValue(), }) //
						.collect(As::streamlet);

				String[] dates = arrays.map(array -> array[0]).toArray(String.class);
				float[] prices = arrays.map(array -> array[1]).collect(As.arrayOfFloats(Float::parseFloat));
				return new DataSource(dates, prices);
			}
		});
	}

	/**
	 * http://www.jarloo.com/yahoo_finance/
	 * 
	 * @return quotes of many stock at once.
	 */
	public synchronized Map<String, Float> quote(Set<String> symbols) {
		return quote(symbols, "l1"); // last price
	}

	public synchronized Map<String, Float> quoteOpenPrice(Set<String> symbols) {
		return quote(symbols, "o");
	}

	private Map<String, Float> quote(Set<String> symbols, String field) {
		Map<String, Float> quotes = quotesByField.computeIfAbsent(field, f -> new HashMap<>());
		Streamlet<String> querySymbols = Read.from(symbols) //
				.filter(symbol -> !Trade_.isCacheQuotes || !quotes.containsKey(symbol)) //
				.distinct();
		quotes.putAll(quote_(querySymbols, field));
		return Read.from(symbols).map2(quotes::get).toMap();
	}

	private static Map<String, Map<String, Float>> quotesByField = new HashMap<>();

	private Map<String, Float> quote_(Streamlet<String> symbols, String field) {
		if (0 < symbols.size()) {
			String urlString = "https://download.finance.yahoo.com/d/quotes.csv" //
					+ "?s=" + symbols.sort(Object_::compare).map(this::encode).collect(As.joined("+")) //
					+ "&f=s" + field;

			URL url = To.url(urlString);
			return HttpUtil.get(url).out.collect(As::csv).toMap(array -> array[0], array -> Float.parseFloat(array[1]));
		} else
			return new HashMap<>();
	}

	public String tableUrl(String symbol, TimeRange period) {
		Time frDate = period.from;
		Time toDate = period.to;
		return "https://chart.finance.yahoo.com/table.csv" //
				+ "?s=" + encode(symbol) //
				+ "&a=" + frDate.month() + "&b=" + frDate.dow() + "&c=" + frDate.year() //
				+ "&d=" + toDate.month() + "&e=" + toDate.dow() + "&f=" + toDate.year() //
				+ "&g=d" //
				+ "&ignore=.csv";
	}

	private void adjust(String symbol, String[] dates, float[] prices) {
		Map<String, BiFunction<String, Float, Float>> adjusters = new HashMap<>();
		adjusters.put("0700.HK", (d, p) -> d.compareTo("2014-05-14") <= 0 ? p * .2f : p);
		adjusters.put("2318.HK", (d, p) -> d.compareTo("2014-03-23") <= 0 ? p * .5f : p);

		BiFunction<String, Float, Float> adjuster = adjusters.get(symbol);
		if (adjuster != null)
			for (int d = 0; d < prices.length; d++)
				prices[d] = adjuster.apply(dates[d], prices[d]);
	}

	private String encode(String s) {
		return Rethrow.ex(() -> URLEncoder.encode(s, Constants.charset.name()));
	}

}
