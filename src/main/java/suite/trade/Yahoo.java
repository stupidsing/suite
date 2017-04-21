package suite.trade;

import java.net.URL;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;

import suite.http.HttpUtil;
import suite.node.util.Singleton;
import suite.streamlet.As;
import suite.streamlet.Read;
import suite.streamlet.Streamlet;
import suite.util.Rethrow;
import suite.util.Util;

public class Yahoo {

	public DataSource dataSource(String stockCode) {
		return dataSource(stockCode, Period.ages());
	}

	public DataSource dataSource(String stockCode, Period period) {
		String urlString = tableUrl(stockCode, period);

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

		adjust(stockCode, dates, prices);

		DataSource dataSource = new DataSource(dates, prices);
		dataSource.cleanse();
		dataSource.validate();

		return dataSource;
	}

	/**
	 * http://www.jarloo.com/yahoo_finance/
	 * 
	 * @return quotes of many stock at once.
	 */
	public Map<String, Float> quote(Streamlet<String> stockCodes) {
		String urlString = quoteUrl(stockCodes, "l1"); // last price

		URL url = Rethrow.ex(() -> new URL(urlString));

		return HttpUtil.http("GET", url).out //
				.collect(As::csv) //
				.toMap(array -> array[0], array -> Float.parseFloat(array[1]));
	}

	public Map<String, Float> quoteOpenPrice(Streamlet<String> stockCodes) {
		String urlString = quoteUrl(stockCodes, "o");

		URL url = Rethrow.ex(() -> new URL(urlString));

		return HttpUtil.http("GET", url).out //
				.collect(As::csv) //
				.toMap(array -> array[0], array -> Float.parseFloat(array[1]));
	}

	private String quoteUrl(Streamlet<String> stockCodes, String field) {
		return "https://download.finance.yahoo.com/d/quotes.csv" //
				+ "?s=" + stockCodes.collect(As.joined("+")) //
				+ "&f=s" + field;
	}

	public String tableUrl(String stockCode, Period period) {
		LocalDate frDate = period.frDate;
		LocalDate toDate = period.toDate;
		return "https://chart.finance.yahoo.com/table.csv" //
				+ "?s=" + stockCode //
				+ "&a=" + frDate.getMonthValue() + "&b=" + frDate.getDayOfMonth() + "&c=" + frDate.getYear() //
				+ "&d=" + toDate.getMonthValue() + "&e=" + toDate.getDayOfMonth() + "&f=" + toDate.getYear() //
				+ "&g=d&ignore=.csv";
	}

	private void adjust(String stockCode, String[] dates, float[] prices) {
		Map<String, BiFunction<String, Float, Float>> adjusters = new HashMap<>();
		adjusters.put("0700.HK", (d, p) -> d.compareTo("2014-05-14") <= 0 ? p * .2f : p);
		adjusters.put("2318.HK", (d, p) -> d.compareTo("2014-03-23") <= 0 ? p * .5f : p);

		BiFunction<String, Float, Float> adjuster = adjusters.get(stockCode);
		if (adjuster != null)
			for (int d = 0; d < prices.length; d++)
				prices[d] = adjuster.apply(dates[d], prices[d]);
	}

}
