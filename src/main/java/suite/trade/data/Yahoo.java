package suite.trade.data;

import static primal.statics.Fail.fail;
import static primal.statics.Rethrow.ex;

import java.net.URLEncoder;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import primal.MoreVerbs.Read;
import primal.Nouns.Utf8;
import primal.Verbs.Compare;
import primal.Verbs.Equals;
import primal.Verbs.New;
import primal.Verbs.WriteFile;
import primal.primitive.FltMoreVerbs.LiftFlt;
import primal.primitive.LngMoreVerbs.LiftLng;
import primal.primitive.adt.pair.LngFltPair;
import primal.streamlet.Streamlet;
import primal.streamlet.Streamlet2;
import suite.cfg.HomeDir;
import suite.http.HttpClient;
import suite.os.LogUtil;
import suite.streamlet.As;
import suite.trade.Time;
import suite.trade.TimeRange;

public class Yahoo {

	private ObjectMapper om = new ObjectMapper();
	private QuoteCache<String> quoteCache = new QuoteCache<>(this::quote_);

	// https://query1.finance.yahoo.com/v8/finance/chart/0012.HK?period1=0&period2=1497550133&interval=1d&indicators=quote&includeTimestamps=true&includePrePost=true&events=div%7Csplit%7Cearn&corsDomain=finance.yahoo.com
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
				var lines = ex(() -> Files.readAllLines(path));
				stockHistory0 = StockHistory.of(Read.from(lines).puller());
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
					.collect(LiftLng.of(t -> getOpenTimeBefore(exchange, t.longValue()))) //
					.toArray();

			var length = ts.length;

			var dataJsons0 = Read //
					.<String>empty() //
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
					.mapValue(json_ -> json_.collect(LiftFlt.of(JsonNode::floatValue)).toArray()) //
					.filterValue(fs -> length <= fs.length) //
					.mapValue(fs -> New.array(length, LngFltPair.class, i -> LngFltPair.of(ts[i], fs[i]))) //
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
				stockHistory1 = fail();

			WriteFile.to(path).writeAndClose(stockHistory1.write());
		} else
			stockHistory1 = stockHistory0;

		Predicate<LngFltPair> splitFilter;
		LngFltPair[] splits2;

		if (Equals.string(symbol, "0700.HK"))
			splitFilter = pair -> pair.t0 != Time.of(2014, 5, 15, 9, 30).epochSec();
		else if (Equals.string(symbol, "2318.HK"))
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
				+ "https://query1.finance.yahoo.com/v8/finance/chart/" //
				+ encode(symbol) //
				+ "?period1=" + period.fr.epochSec() //
				+ "&period2=" + period.to.epochSec() //
				+ "&interval=1d" //
				+ "&includePrePost=true" //
				+ "&events=div%7Csplit%7Cearn" //
				+ "&corsDomain=finance.yahoo.com";

		return HttpClient.get(url).inputStream().doRead(om::readTree);
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
			var cookie = "A1=d=AQABBJ9wzWYCEKqayM1ExKtxXG3sbN4ry0AFEgEBAQHCzmbXZliZ8HgB_eMAAA&S=AQAAAnzHQn5Gwlw1uRk7Wr589m4";
			var userAgent = "Mozilla/5.0 (X11; Linux x86_64; rv:109.0) Gecko/20100101 Firefox/115.0";

			var headers = Map.ofEntries( //
					Map.entry("Cookie", cookie), //
					Map.entry("User-Agent", userAgent));

			var url0 = "https://query2.finance.yahoo.com/v1/test/getcrumb";

			var crumb = HttpClient.get(url0).headers(headers).out().collect(As::string);

			var url1 = "https://query1.finance.yahoo.com/v7/finance/quote" //
					+ "?symbols=" + symbols.sort(Compare::objects).map(this::encode).toJoinedString("+") //
					+ "&crumb=" + crumb;

			var json = HttpClient.get(url1).headers(headers).inputStream().doRead(om::readTree);

			var jsons = Read.each(json) //
					.flatMap(json_ -> json_.path("quoteResponse").path("result"));

			return jsons //
					.map2( //
							json_ -> json_.path("symbol").textValue(), //
							json_ -> json_.path("regularMarketPrice").numberValue().floatValue()) //
					.toMap();
		} else
			return new HashMap<>();
	}

	private long getOpenTimeBefore(String exchange, long t) {
		return !isHkg(exchange) ? t : HkexUtil.getOpenTimeBefore(Time.ofEpochSec(t)).epochSec();
	}

	private long getTradeTimeBefore(String exchange, long t) {
		return !isHkg(exchange) ? t : HkexUtil.getTradeTimeBefore(Time.ofEpochSec(t)).epochSec();
	}

	private boolean isHkg(String exchange) {
		return Equals.string(exchange, "HKG");
	}

	private String encode(String s) {
		return ex(() -> URLEncoder.encode(s, Utf8.charset.name()));
	}

}
