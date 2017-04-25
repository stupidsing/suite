package suite.trade;

import java.util.Arrays;

import suite.util.FormatUtil;
import suite.util.Util;

public class DataSource {

	public final String[] dates;
	public final float[] prices;

	public DataSource(String[] dates, float[] prices) {
		this.dates = dates;
		this.prices = prices;
	}

	public void cleanse() {

		// ignore price sparks caused by data source bugs
		for (int i = 2; i < prices.length; i++) {
			float price0 = prices[i - 2];
			float price1 = prices[i - 1];
			float price2 = prices[i - 0];
			if (isValid(price0, price2) && !isValid(price0, price1) && !isValid(price1, price2))
				prices[i - 1] = price0;
		}
	}

	public DataSource limit(Period period) {
		String s0 = FormatUtil.dateFormat.format(period.frDate);
		String sx = FormatUtil.dateFormat.format(period.toDate);

		String[] dates1 = new String[dates.length];
		float[] prices1 = new float[prices.length];
		int j = 0;
		for (int i = 0; i < prices.length; i++) {
			String date = dates[i];
			float price = prices[i];
			if (Util.compare(s0, date) <= 0 && Util.compare(date, sx) < 0) {
				dates1[j] = date;
				prices1[j] = price;
				j++;
			}
		}

		return new DataSource(Arrays.copyOf(dates1, j), Arrays.copyOf(prices, j));
	}

	public void validate() {
		int length = prices.length;
		String date0 = null;
		float price0 = prices[0];
		float price1;

		for (int i = 1; i < length; i++) {
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
