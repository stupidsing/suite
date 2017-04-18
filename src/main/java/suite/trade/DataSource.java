package suite.trade;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import suite.os.LogUtil;
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

	public void validate() {
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

		if (!Util.stringEquals(date0, DateTimeFormatter.ofPattern("yyyy-MM-dd").format(LocalDate.now())))
			LogUtil.warn("outdated date range: " + date0);
	}

	private boolean isValid(float price0, float price1) {
		float ratio = price1 / price0;
		return 1f / 2f < ratio && ratio < 2f / 1f;
	}

}
