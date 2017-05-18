package suite.trade.data;

import java.io.InputStream;
import java.net.URL;
import java.net.URLEncoder;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiFunction;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import suite.Constants;
import suite.http.HttpUtil;
import suite.node.util.Singleton;
import suite.streamlet.As;
import suite.streamlet.Read;
import suite.streamlet.Streamlet;
import suite.trade.DatePeriod;
import suite.util.Object_;
import suite.util.Rethrow;
import suite.util.To;

public class Yahoo {

	private static ObjectMapper mapper = new ObjectMapper();

	public DataSource dataSourceCsv(String symbol, DatePeriod period) {
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

	public DataSource dataSourceL1(String symbol, DatePeriod period) {
		String urlString = "https://l1-query.finance.yahoo.com/v7/finance/chart/" //
				+ encode(symbol) //
				+ "?period1=" + period.from.atStartOfDay().toEpochSecond(ZoneOffset.UTC) //
				+ "&period2=" + period.to.atStartOfDay().toEpochSecond(ZoneOffset.UTC) //
				+ "&interval=1d" //
				+ "&indicators=quote" //
				+ "&includeTimestamps=true" //
				+ "&includePrePost=true" //
				+ "&events=div%7Csplit%7Cearn" //
				+ "&corsDomain=finance.yahoo.com";

		return Rethrow.ex(() -> {
			try (InputStream is = Singleton.get().getStoreCache().http(urlString).collect(To::inputStream)) {
				JsonNode json = mapper.readTree(is);

				Streamlet<JsonNode> jsons = Read.each(json) //
						.flatMap(json_ -> json_.get("chart")) //
						.flatMap(json_ -> json_);

				String[] dates = jsons //
						.flatMap(json_ -> json_.get("timestamp")) //
						.map(json_ -> LocalDateTime.ofEpochSecond(json_.intValue(), 0, ZoneOffset.UTC).toLocalDate()) //
						.map(To::string) //
						.toArray(String.class);

				float[] prices = jsons //
						.flatMap(json_ -> json_.get("indicators").get("quote")) //
						.flatMap(json_ -> json_.get("open")) //
						.collect(As.arrayOfFloats(JsonNode::floatValue));

				return new DataSource(dates, prices);
			}
		});
	}

	public DataSource dataSourceYql(String symbol, DatePeriod period) {
		String yql = "select *" //
				+ " from yahoo.finance.historicaldata" //
				+ " where symbol = \"" + symbol + "\"" //
				+ " and startDate = \"" + To.string(period.from) + "\"" //
				+ " and endDate = \"" + To.string(period.to) + "\"";

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
		Streamlet<String> querySymbols = Read.from(symbols).filter(symbol -> !quotes.containsKey(symbol)).distinct();
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

	public String tableUrl(String symbol, DatePeriod period) {
		LocalDate frDate = period.from;
		LocalDate toDate = period.to;
		return "https://chart.finance.yahoo.com/table.csv" //
				+ "?s=" + encode(symbol) //
				+ "&a=" + frDate.getMonthValue() + "&b=" + frDate.getDayOfMonth() + "&c=" + frDate.getYear() //
				+ "&d=" + toDate.getMonthValue() + "&e=" + toDate.getDayOfMonth() + "&f=" + toDate.getYear() //
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
