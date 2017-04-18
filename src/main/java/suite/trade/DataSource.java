package suite.trade;

import java.time.LocalDate;
import java.util.List;

import suite.Constants;
import suite.node.util.Singleton;
import suite.primitive.Bytes;
import suite.primitive.BytesUtil;
import suite.streamlet.As;
import suite.streamlet.Read;
import suite.util.Util;

public class DataSource {

	public final String[] dates;
	public final float[] prices;

	public static DataSource yahoo(String stockCode) {
		LocalDate frDate = LocalDate.of(1980, 1, 1);
		LocalDate toDate = LocalDate.of(2020, 1, 1);
		return yahoo(stockCode, frDate, toDate);
	}

	public static DataSource yahoo(String stockCode, LocalDate frDate, LocalDate toDate) {
		String urlString = yahooUrl(stockCode, frDate, toDate);

		// Date, Open, High, Low, Close, Volume, Adj Close
		List<String[]> arrays = Singleton.get() //
				.getStoreCache() //
				.http(urlString) //
				.collect(BytesUtil.split(Bytes.of((byte) 10))) //
				.skip(1) //
				.map(bytes -> new String(bytes.toBytes(), Constants.charset).split(",")) //
				.sort((a0, a1) -> Util.compare(a0[0], a1[0])) //
				.toList();

		String[] dates = Read.from(arrays) //
				.map(array -> array[0]) //
				.toArray(String.class);

		float[] prices = Read.from(arrays) //
				.collect(As.arrayOfFloats(array -> Float.parseFloat(array[1])));

		DataSource dataSource = new DataSource(dates, prices);
		dataSource.cleanse();
		dataSource.validate();

		return dataSource;
	}

	public static String yahooUrl(String stockCode, LocalDate frDate, LocalDate toDate) {
		return "http://chart.finance.yahoo.com/table.csv" //
				+ "?s=" + stockCode //
				+ "&a=" + frDate.getMonthValue() + "&b=" + frDate.getDayOfMonth() + "&c=" + frDate.getYear() //
				+ "&d=" + toDate.getMonthValue() + "&e=" + toDate.getDayOfMonth() + "&f=" + toDate.getYear() //
				+ "&g=d&ignore=.csv";
	}

	private DataSource(String[] dates, float[] prices) {
		this.dates = dates;
		this.prices = prices;
	}

	private void cleanse() {

		// ignore price sparks caused by data source bugs
		for (int i = 2; i < prices.length; i++) {
			float price0 = prices[i - 2];
			float price1 = prices[i - 1];
			float price2 = prices[i - 0];
			if (isValid(price0, price2) && !isValid(price0, price1) && !isValid(price1, price2))
				prices[i - 1] = price0;
		}
	}

	private void validate() {
		float price0 = prices[0];
		float price1;
		String date0 = null;

		for (int i = 1; i < prices.length; i++) {
			String date1 = dates[i];

			if ((price1 = prices[i]) == 0f)
				throw new RuntimeException("Price is zero: " + price1 + "/" + date1);

			if (!Float.isFinite(price1))
				throw new RuntimeException("Price is not finite: " + price1 + "/" + date1);

			boolean valid = isValid(price0, price1);
			if (!valid)
				throw new RuntimeException("Price varied too much: " + price0 + "/" + date0 + " => " + price1 + "/" + date1);

			date0 = date1;
			price0 = price1;
		}
	}

	private boolean isValid(float price0, float price1) {
		float ratio = price1 / price0;
		return 1f / 2f < ratio && ratio < 2f / 1f;
	}

}
