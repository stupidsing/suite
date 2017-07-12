package suite.trade.data;

import suite.primitive.IntFlt_Flt;
import suite.primitive.Int_Flt;
import suite.primitive.adt.pair.LngFltPair;

/**
 * Eliminate price sparks caused by data source bugs.
 * 
 * @author ywsing
 */
public class Cleanse {

	public float[] cleanse(float[] prices) {
		cleanse(prices.length, //
				i -> prices[i], //
				(i, price) -> prices[i] = price);
		return prices;
	}

	public LngFltPair[] cleanse(LngFltPair[] pairs) {
		cleanse(pairs.length, //
				i -> pairs[i].t1, //
				(i, price) -> pairs[i].t1 = price);
		return pairs;
	}

	public boolean isValid(float price0, float price1) {
		return isValid_(price0, price1);
	}

	public float[] removeZeroes(float[] prices0) {
		int length = prices0.length;
		float[] prices1 = new float[length];
		float price0 = 0f;
		for (int i = 0; i < length; i++) {
			float price = prices0[i];
			prices1[i] = price0 = price != 0f ? price : price0;
		}
		return prices1;
	}

	private void cleanse(int length, Int_Flt get, IntFlt_Flt set) {

		// fill zeroes in the middle
		float ppos = 0f;
		int i0 = 0;
		for (int i = 0; i < length; i++) {
			float price = get.apply(i);
			if (price != 0f) {
				while (i0 < i)
					set.apply(i0++, ppos);
				ppos = price;
			}
		}

		// eliminate spikes
		for (int i = 2; i < length; i++) {
			float price0 = get.apply(i - 2);
			float price1 = get.apply(i - 1);
			float price2 = get.apply(i - 0);
			if (isValid(price0, price2) && !isValid(price0, price1) && !isValid(price1, price2))
				set.apply(i - 1, price0);
		}
	}

	private boolean isValid_(float price0, float price1) {
		float ratio = price1 / price0;
		return Float.isFinite(ratio) && 1f / 2f < ratio && ratio < 2f / 1f;
	}

}
