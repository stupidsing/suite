package suite.trade.data;

import java.net.URL;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiFunction;

import suite.http.HttpUtil;
import suite.node.util.Singleton;
import suite.streamlet.As;
import suite.streamlet.Read;
import suite.streamlet.Streamlet;
import suite.trade.DatePeriod;
import suite.util.To;
import suite.util.Util;

public class Yahoo {

	public DataSource dataSource(String symbol) {
		return dataSource(symbol, DatePeriod.ages());
	}

	public DataSource dataSource(String symbol, DatePeriod period) {
		String urlString = tableUrl(symbol, period);

		// Date, Open, High, Low, Close, Volume, Adj Close
		List<String[]> arrays = Singleton.get() //
				.getStoreCache() //
				.http(urlString) //
				.collect(As::csv) //
				.skip(1) //
				.sort((a0, a1) -> Util.compare(a0[0], a1[0])) //
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
		Set<String> querySymbols = Read.from(symbols).filter(symbol -> !quotes.containsKey(symbol)).toSet();
		quotes.putAll(quote0(Read.from(querySymbols), field));
		return Read.from(symbols).map2(quotes::get).toMap();
	}

	private static Map<String, Map<String, Float>> quotesByField = new HashMap<>();

	private Map<String, Float> quote0(Streamlet<String> symbols, String field) {
		if (0 < symbols.size()) {
			String urlString = quoteUrl(symbols, field);
			URL url = To.url(urlString);
			return HttpUtil.get(url).out.collect(As::csv).toMap(array -> array[0], array -> Float.parseFloat(array[1]));
		} else
			return new HashMap<>();
	}

	private String quoteUrl(Streamlet<String> symbols, String field) {
		return "https://download.finance.yahoo.com/d/quotes.csv" //
				+ "?s=" + symbols.sort(Util::compare).collect(As.joined("+")) //
				+ "&f=s" + field;
	}

	public String tableUrl(String symbol, DatePeriod period) {
		LocalDate frDate = period.from;
		LocalDate toDate = period.to;
		return "https://chart.finance.yahoo.com/table.csv" //
				+ "?s=" + symbol //
				+ "&a=" + frDate.getMonthValue() + "&b=" + frDate.getDayOfMonth() + "&c=" + frDate.getYear() //
				+ "&d=" + toDate.getMonthValue() + "&e=" + toDate.getDayOfMonth() + "&f=" + toDate.getYear() //
				+ "&g=d&ignore=.csv";
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

}
