package suite.trade.data;

import java.net.URLEncoder;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import suite.Defaults;
import suite.http.HttpUtil;
import suite.node.util.Singleton;
import suite.os.FileUtil;
import suite.os.LogUtil;
import suite.primitive.FltPrimitives.Obj_Flt;
import suite.primitive.LngPrimitives.Obj_Lng;
import suite.primitive.adt.pair.LngFltPair;
import suite.streamlet.As;
import suite.streamlet.Read;
import suite.streamlet.Streamlet;
import suite.streamlet.Streamlet2;
import suite.trade.Time;
import suite.trade.TimeRange;
import suite.util.Fail;
import suite.util.FunUtil2.FoldOp;
import suite.util.HomeDir;
import suite.util.Object_;
import suite.util.Rethrow;
import suite.util.String_;
import suite.util.To;

public class Yahoo {

	private static ObjectMapper mapper = new ObjectMapper();

	private Cleanse cleanse = new Cleanse();
	private QuoteCache<String> quoteCache = new QuoteCache<>(this::quote_);

	public DataSource dataSourceCsv(String symbol, TimeRange period) {
		var urlString = tableUrl(symbol, period);

		// Date, Open, High, Low, Close, Volume, Adj Close
		var arrays = Singleton.me.storeCache //
				.http(urlString) //
				.collect(As::csv) //
				.skip(1) //
				.sort((a0, a1) -> Object_.compare(a0[0], a1[0])) //
				.collect();

		var ts = arrays.collect(Obj_Lng.lift(array -> closeTs(array[0]))).toArray();
		var opens = arrays.collect(Obj_Flt.lift(array -> Float.parseFloat(array[1]))).toArray();
		var closes = arrays.collect(Obj_Flt.lift(array -> Float.parseFloat(array[4]))).toArray();
		var lows = arrays.collect(Obj_Flt.lift(array -> Float.parseFloat(array[3]))).toArray();
		var highs = arrays.collect(Obj_Flt.lift(array -> Float.parseFloat(array[2]))).toArray();
		var volumes = arrays.collect(Obj_Flt.lift(array -> Float.parseFloat(array[5]))).toArray();

		adjust(symbol, ts, opens);
		adjust(symbol, ts, closes);
		adjust(symbol, ts, lows);
		adjust(symbol, ts, highs);

		return DataSource.ofOhlcv( //
				ts, //
				cleanse.cleanse(opens), //
				cleanse.cleanse(closes), //
				cleanse.cleanse(lows), //
				cleanse.cleanse(highs), //
				volumes);
	}

	// https://l1-query.finance.yahoo.com/v7/finance/chart/0012.HK?period1=0&period2=1497550133&interval=1d&indicators=quote&includeTimestamps=true&includePrePost=true&events=div%7Csplit%7Cearn&corsDomain=finance.yahoo.com
	public DataSource dataSourceL1(String symbol, TimeRange period) {
		var stockHistory = getStockHistory(symbol);
		var ds = stockHistory.filter(period).toDataSource();
		var ts = ds.ts;

		// the latest time stamp may fluctuate; adjust it to previous market
		// close time
		if (0 < ts.length) {
			var last = ts.length - 1;
			ts[last] = getTradeTimeBefore(stockHistory.exchange, ts[last]);
		}

		return ds.cleanse().range(period);
	}

	public LngFltPair[] dividend(String symbol) {
		return getStockHistory(symbol).dividends;
	}

	private StockHistory getStockHistory(String symbol) {
		var path = HomeDir.dir("yahoo").resolve(symbol + ".txt");
		StockHistory stockHistory0;

		if (Files.exists(path))
			try {
				var lines = Rethrow.ex(() -> Files.readAllLines(path));
				stockHistory0 = StockHistory.of(Read.from(lines).outlet());
			} catch (Exception ex) {
				stockHistory0 = StockHistory.new_();
			}
		else
			stockHistory0 = StockHistory.new_();

		var time = HkexUtil.getCloseTimeBefore(Time.now());
		StockHistory stockHistory1;

		if (stockHistory0.isActive && Time.compare(stockHistory0.time, time) < 0) {
			var json = queryL1(symbol, TimeRange.of(stockHistory0.time.addDays(-14), Time.now()));

			var jsons = Read.each(json) //
					.flatMap(json_ -> json_.path("chart").path("result"));

			var exchange = jsons //
					.map(json_ -> json_.path("meta").path("exchangeName").textValue()) //
					.uniqueResult();

			var ts = jsons //
					.flatMap(json_ -> json_.path("timestamp")) //
					.collect(Obj_Lng.lift(t -> getOpenTimeBefore(exchange, t.longValue()))) //
					.toArray();

			var length = ts.length;

			var dataJsons0 = Read //
					.<String> empty() //
					.map2(tag -> jsons //
							.flatMap(json_ -> {
								var json0 = json_.path("indicators");
								JsonNode json1;
								if (false //
										|| !(json1 = json0.path("unadjclose")).isMissingNode() //
										|| !(json1 = json0.path("unadjquote")).isMissingNode())
									return json1;
								else
									return List.of();
							}) //
							.flatMap(json_ -> json_.path("unadj" + tag)));

			var dataJsons1 = Read //
					.each("open", "close", "high", "low", "volume") //
					.map2(tag -> jsons //
							.flatMap(json_ -> json_.path("indicators").path("quote")) //
							.flatMap(json_ -> json_.path(tag)));

			var data = Streamlet2 //
					.concat(dataJsons0, dataJsons1) //
					.mapValue(json_ -> json_.collect(Obj_Flt.lift(JsonNode::floatValue)).toArray()) //
					.filterValue(fs -> length <= fs.length) //
					.mapValue(fs -> To.array(length, LngFltPair.class, i -> LngFltPair.of(ts[i], fs[i]))) //
					.toMap();

			var dividends = jsons //
					.flatMap(json_ -> json_.path("events").path("dividends")) //
					.map(json_ -> LngFltPair.of(json_.path("date").longValue(), json_.path("amount").floatValue())) //
					.sort(LngFltPair.comparatorByFirst()) //
					.toArray(LngFltPair.class);

			var splits = jsons //
					.flatMap(json_ -> json_.path("events").path("splits")) //
					.map(json_ -> LngFltPair.of(json_.path("date").longValue(),
							json_.path("numerator").floatValue() / json_.path("denominator").floatValue())) //
					.sort(LngFltPair.comparatorByFirst()) //
					.toArray(LngFltPair.class);

			if (data.containsKey("close"))
				stockHistory1 = StockHistory //
						.of(exchange, time, true, data, dividends, splits) //
						.merge(stockHistory0) //
						.alignToDate();
			else
				stockHistory1 = Fail.t();

			FileUtil.write(path, stockHistory1.write());
		} else
			stockHistory1 = stockHistory0;

		Predicate<LngFltPair> splitFilter;
		LngFltPair[] splits2;

		if (String_.equals(symbol, "0700.HK"))
			splitFilter = pair -> pair.t0 != Time.of(2014, 5, 15, 9, 30).epochSec();
		else if (String_.equals(symbol, "2318.HK"))
			splitFilter = pair -> pair.t0 != Time.of(2015, 7, 27, 9, 30).epochSec();
		else
			splitFilter = null;

		splits2 = splitFilter != null //
				? Read.from(stockHistory1.splits).filter(splitFilter).toArray(LngFltPair.class) //
				: stockHistory1.splits;

		var stockHistory2 = stockHistory1.create(stockHistory1.data, stockHistory1.dividends, splits2);
		var stockHistory3 = LogUtil.prefix("for " + symbol + ": ", () -> stockHistory2.cleanse());
		return stockHistory3;
	}

	private JsonNode queryL1(String symbol, TimeRange period) {
		var url = "" //
				+ "https://l1-query.finance.yahoo.com/v7/finance/chart/" //
				+ encode(symbol) //
				+ "?period1=" + period.from.epochSec() //
				+ "&period2=" + period.to.epochSec() //
				+ "&interval=1d" //
				+ "&indicators=quote" //
				+ "&includeTimestamps=true" //
				+ "&includePrePost=true" //
				+ "&events=div%7Csplit%7Cearn" //
				+ "&corsDomain=finance.yahoo.com";

		return Rethrow.ex(() -> {
			try (var is = HttpUtil.get(url).inputStream()) {
				return mapper.readTree(is);
			}
		});
	}

	public DataSource dataSourceYql(String symbol, TimeRange period) {
		var yql = "select *" //
				+ " from yahoo.finance.historicaldata" //
				+ " where symbol = \"" + symbol + "\"" //
				+ " and startDate = \"" + period.from.ymd() + "\"" //
				+ " and endDate = \"" + period.to.ymd() + "\"";

		var urlString = "http://query.yahooapis.com/v1/public/yql" //
				+ "?q=" + encode(yql) //
				+ "&format=json" //
				+ "&diagnostics=true" //
				+ "&env=store%3A%2F%2Fdatatables.org%2Falltableswithkeys" //
				+ "&callback=";

		return Rethrow.ex(() -> {
			try (var is = Singleton.me.storeCache.http(urlString).collect(To::inputStream)) {
				var json = mapper.readTree(is);

				var quotes = Read.each(json) //
						.flatMap(json_ -> json_.path("query")) //
						.flatMap(json_ -> json_.path("results")) //
						.flatMap(json_ -> json_.path("quote")) //
						.collect();

				var arrays = quotes //
						.map(json_ -> new String[] { //
								json_.path("Date").textValue(), //
								json_.path("Open").textValue(), //
								json_.path("Close").textValue(), //
								json_.path("Low").textValue(), //
								json_.path("High").textValue(), }) //
						.collect();

				var ts = arrays.collect(Obj_Lng.lift(array -> closeTs(array[0]))).toArray();
				var opens = arrays.collect(Obj_Flt.lift(array -> Float.parseFloat(array[1]))).toArray();
				var closes = arrays.collect(Obj_Flt.lift(array -> Float.parseFloat(array[2]))).toArray();
				var lows = arrays.collect(Obj_Flt.lift(array -> Float.parseFloat(array[3]))).toArray();
				var highs = arrays.collect(Obj_Flt.lift(array -> Float.parseFloat(array[4]))).toArray();
				var volumes = new float[ts.length];
				return DataSource.ofOhlcv(ts, opens, closes, lows, highs, volumes);
			}
		});
	}

	/**
	 * http://www.jarloo.com/yahoo_finance/
	 * 
	 * @return quotes of many stock at once.
	 */
	public Map<String, Float> quote(Set<String> symbols) {
		return quote(symbols, "l1"); // last price
		// "o" - open
	}

	private Map<String, Float> quote(Set<String> symbols, String field) {
		return quoteCache.quote(symbols, field);
	}

	private Map<String, Float> quote_(Streamlet<String> symbols, String field) {
		if (0 < symbols.size()) {
			var url = "https://download.finance.yahoo.com/d/quotes.csv" //
					+ "?s=" + symbols.sort(Object_::compare).map(this::encode).collect(As.joinedBy("+")) //
					+ "&f=s" + field;

			return HttpUtil.get(url).out().collect(As::csv).toMap(array -> array[0], array -> Float.parseFloat(array[1]));
		} else
			return new HashMap<>();
	}

	public String tableUrl(String symbol, TimeRange period) {
		var frDate = period.from;
		var toDate = period.to;
		return "https://chart.finance.yahoo.com/table.csv" //
				+ "?s=" + encode(symbol) //
				+ "&a=" + frDate.month() + "&b=" + frDate.dow() + "&c=" + frDate.year() //
				+ "&d=" + toDate.month() + "&e=" + toDate.dow() + "&f=" + toDate.year() //
				+ "&g=d" //
				+ "&ignore=.csv";
	}

	private void adjust(String symbol, long[] ts, float[] prices) {
		var adjusters = new HashMap<String, FoldOp<Long, Float>>();
		adjusters.put("0700.HK", (d, p) -> String_.compare(Time.ofEpochSec(d).ymd(), "2014-05-14") <= 0 ? p * .2f : p);
		adjusters.put("2318.HK", (d, p) -> String_.compare(Time.ofEpochSec(d).ymd(), "2014-03-23") <= 0 ? p * .5f : p);

		var adjuster = adjusters.get(symbol);
		if (adjuster != null)
			for (var d = 0; d < prices.length; d++)
				prices[d] = adjuster.apply(ts[d], prices[d]);
	}

	private long getOpenTimeBefore(String exchange, long t) {
		return !isHkg(exchange) ? t : HkexUtil.getOpenTimeBefore(Time.ofEpochSec(t)).epochSec();
	}

	private long getTradeTimeBefore(String exchange, long t) {
		return !isHkg(exchange) ? t : HkexUtil.getTradeTimeBefore(Time.ofEpochSec(t)).epochSec();
	}

	private boolean isHkg(String exchange) {
		return String_.equals(exchange, "HKG");
	}

	private long closeTs(String ymd) {
		return Time.of(ymd + " 16:30:00").epochSec();
	}

	private String encode(String s) {
		return Rethrow.ex(() -> URLEncoder.encode(s, Defaults.charset.name()));
	}

}
