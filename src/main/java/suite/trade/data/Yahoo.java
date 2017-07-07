package suite.trade.data;

import java.io.InputStream;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.file.Files;
import java.nio.file.Path;
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
import suite.primitive.FltPrimitives.Obj_Flt;
import suite.primitive.LngPrimitives.Obj_Lng;
import suite.primitive.adt.pair.LngFltPair;
import suite.streamlet.As;
import suite.streamlet.Read;
import suite.streamlet.Streamlet;
import suite.streamlet.Streamlet2;
import suite.trade.Time;
import suite.trade.TimeRange;
import suite.trade.Trade_;
import suite.util.HomeDir;
import suite.util.Object_;
import suite.util.Rethrow;
import suite.util.String_;
import suite.util.To;

public class Yahoo {

	private static ObjectMapper mapper = new ObjectMapper();

	private Cleanse cleanse = new Cleanse();

	public DataSource dataSourceCsv(String symbol, TimeRange period) {
		String urlString = tableUrl(symbol, period);

		// Date, Open, High, Low, Close, Volume, Adj Close
		Streamlet<String[]> arrays = Singleton.get() //
				.getStoreCache() //
				.http(urlString) //
				.collect(As::csv) //
				.skip(1) //
				.sort((a0, a1) -> Object_.compare(a0[0], a1[0])) //
				.collect(As::streamlet);

		long[] ts = arrays //
				.collect(Obj_Lng.lift(array -> closeTs(array[0]))) //
				.toArray();

		float[] opens = arrays //
				.collect(Obj_Flt.lift(array -> Float.parseFloat(array[1]))) //
				.toArray();

		float[] closes = arrays //
				.collect(Obj_Flt.lift(array -> Float.parseFloat(array[4]))) //
				.toArray();

		adjust(symbol, ts, opens);
		adjust(symbol, ts, closes);

		DataSource ds = DataSource.of(ts, cleanse.cleanse(closes));
		return ds;
	}

	// https://l1-query.finance.yahoo.com/v7/finance/chart/0012.HK?period1=0&period2=1497550133&interval=1d&indicators=quote&includeTimestamps=true&includePrePost=true&events=div%7Csplit%7Cearn&corsDomain=finance.yahoo.com
	public DataSource dataSourceL1(String symbol, TimeRange period) {
		Path path = HomeDir.dir("yahoo").resolve(symbol + ".txt");
		StockHistory stockHistory0;

		if (Files.exists(path)) {
			List<String> lines = Rethrow.ex(() -> Files.readAllLines(path));
			stockHistory0 = StockHistory.of(Read.from(lines).outlet());
		} else
			stockHistory0 = StockHistory.new_();

		Time time = HkexUtil.getCloseTimeBefore(Time.now());
		StockHistory stockHistory1;

		if (Time.compare(stockHistory0.time, time) < 0) {
			JsonNode json = queryL1(symbol, TimeRange.of(stockHistory0.time.addDays(-14), Time.now()));

			Streamlet<JsonNode> jsons = Read.each(json) //
					.flatMap(json_ -> json_.path("chart").path("result"));

			String exchange = jsons //
					.map(json_ -> json_.path("meta").path("exchangeName").textValue()) //
					.uniqueResult();

			long[] ts = jsons //
					.flatMap(json_ -> json_.path("timestamp")) //
					.collect(Obj_Lng.lift(t -> getOpenTimeBefore(exchange, t.longValue()))) //
					.toArray();

			int length = ts.length;

			Streamlet2<String, Streamlet<JsonNode>> dataJsons0 = Read //
					.each("open", "close", "high", "low") //
					.map2(tag -> jsons //
							.flatMap(json_ -> json_.path("indicators").path("unadjquote")) //
							.flatMap(json_ -> json_.path("unadj" + tag)));

			Streamlet2<String, Streamlet<JsonNode>> dataJsons1 = Read //
					.each("volume") //
					.map2(tag -> jsons //
							.flatMap(json_ -> json_.path("indicators").path("quote")) //
							.flatMap(json_ -> json_.path(tag)));

			Map<String, LngFltPair[]> data = Streamlet2 //
					.concat(dataJsons0, dataJsons1) //
					.mapValue(json_ -> {
						float[] fs = json_.collect(Obj_Flt.lift(JsonNode::floatValue)).toArray();
						return To.array(LngFltPair.class, length, i -> LngFltPair.of(ts[i], fs[i]));
					}) //
					.toMap();

			LngFltPair[] dividends = jsons //
					.flatMap(json_ -> json_.path("events").path("dividends")) //
					.map(json_ -> LngFltPair.of(json_.path("date").longValue(), json_.path("amount").floatValue())) //
					.sort(LngFltPair.comparatorByFirst()) //
					.toArray(LngFltPair.class);

			LngFltPair[] splits = jsons //
					.flatMap(json_ -> json_.path("events").path("splits")) //
					.map(json_ -> LngFltPair.of(json_.path("date").longValue(),
							json_.path("numerator").floatValue() / json_.path("denominator").floatValue())) //
					.sort(LngFltPair.comparatorByFirst()) //
					.toArray(LngFltPair.class);

			stockHistory1 = StockHistory //
					.of(exchange, time, data, dividends, splits) //
					.merge(stockHistory0) //
					.alignToDate();

			List<String> lines = stockHistory1.write().toList();
			Rethrow.ex(() -> Files.write(path, lines));
		} else
			stockHistory1 = stockHistory0;

		DataSource ds = stockHistory1.cleanse().filter(period).toDataSource();

		// the latest time stamp may fluctuate; adjust it to previous market
		// close time
		long[] ts = ds.ts;
		int last = ts.length - 1;
		ts[last] = getTradeTimeBefore(stockHistory1.exchange, ts[last]);

		return cleanse.cleanse(ds).range(period);
	}

	private JsonNode queryL1(String symbol, TimeRange period) {
		URL url = To.url("" //
				+ "https://l1-query.finance.yahoo.com/v7/finance/chart/" //
				+ encode(symbol) //
				+ "?period1=" + period.from.epochSec() //
				+ "&period2=" + period.to.epochSec() //
				+ "&interval=1d" //
				+ "&indicators=quote" //
				+ "&includeTimestamps=true" //
				+ "&includePrePost=true" //
				+ "&events=div%7Csplit%7Cearn" //
				+ "&corsDomain=finance.yahoo.com");

		return Rethrow.ex(() -> {
			try (InputStream is = HttpUtil.get(url).out.collect(To::inputStream)) {
				return mapper.readTree(is);
			}
		});
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

				Streamlet<JsonNode> quotes = Read.each(json) //
						.flatMap(json_ -> json_.path("query")) //
						.flatMap(json_ -> json_.path("results")) //
						.flatMap(json_ -> json_.path("quote")) //
						.collect(As::streamlet);

				Streamlet<String[]> arrays = quotes //
						.map(json_ -> new String[] { //
								json_.path("Date").textValue(), //
								json_.path("Open").textValue(), //
								json_.path("Close").textValue(), }) //
						.collect(As::streamlet);

				long[] ts = arrays.collect(Obj_Lng.lift(array -> closeTs(array[0]))).toArray();
				float[] opens = arrays.collect(Obj_Flt.lift(array -> Float.parseFloat(array[1]))).toArray();
				float[] closes = arrays.collect(Obj_Flt.lift(array -> Float.parseFloat(array[2]))).toArray();
				return DataSource.ofOpenClose(ts, opens, closes);
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
		// "o" - open
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

	private void adjust(String symbol, long[] ts, float[] prices) {
		Map<String, BiFunction<Long, Float, Float>> adjusters = new HashMap<>();
		adjusters.put("0700.HK", (d, p) -> String_.compare(Time.ofEpochSec(d).ymd(), "2014-05-14") <= 0 ? p * .2f : p);
		adjusters.put("2318.HK", (d, p) -> String_.compare(Time.ofEpochSec(d).ymd(), "2014-03-23") <= 0 ? p * .5f : p);

		BiFunction<Long, Float, Float> adjuster = adjusters.get(symbol);
		if (adjuster != null)
			for (int d = 0; d < prices.length; d++)
				prices[d] = adjuster.apply(ts[d], prices[d]);
	}

	private long getOpenTimeBefore(String exchange, long t) {
		return !String_.equals(exchange, "HKG") ? t : HkexUtil.getOpenTimeBefore(Time.ofEpochSec(t)).epochSec();
	}

	private long getTradeTimeBefore(String exchange, long t) {
		return !String_.equals(exchange, "HKG") ? t : HkexUtil.getTradeTimeBefore(Time.ofEpochSec(t)).epochSec();
	}

	private long closeTs(String ymd) {
		return Time.of(ymd + " 16:30:00").epochSec();
	}

	private String encode(String s) {
		return Rethrow.ex(() -> URLEncoder.encode(s, Constants.charset.name()));
	}

}
