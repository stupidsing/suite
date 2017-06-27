package suite.trade.data;

import suite.primitive.adt.pair.LngFltPair;

/**
 * Eliminate price sparks caused by data source bugs.
 * 
 * @author ywsing
 */
public class Cleanse {

	public DataSource cleanse(DataSource ds) {
		float[] prices = ds.prices;
		cleanse(prices);
		return ds;
	}

	public float[] cleanse(float[] prices) {
		for (int i = 2; i < prices.length; i++) {
			float price0 = prices[i - 2];
			float price1 = prices[i - 1];
			float price2 = prices[i - 0];
			if (isValid(price0, price2) && !isValid(price0, price1) && !isValid(price1, price2))
				prices[i - 1] = price0;
		}
		return prices;
	}

	public LngFltPair[] cleanse(LngFltPair[] pairs) {
		for (int i = 2; i < pairs.length; i++) {
			float price0 = pairs[i - 2].t1;
			float price1 = pairs[i - 1].t1;
			float price2 = pairs[i - 0].t1;
			if (isValid(price0, price2) && !isValid(price0, price1) && !isValid(price1, price2))
				pairs[i - 1].t1 = price0;
		}
		return pairs;
	}

	public boolean isValid(float price0, float price1) {
		float ratio = price1 / price0;
		return Float.isFinite(ratio) && 1f / 2f < ratio && ratio < 2f / 1f;
	}

}
