package suite.trade.data;

import suite.os.Log_;
import suite.primitive.IntFlt_Flt;
import suite.primitive.Int_Flt;
import suite.primitive.adt.pair.LngFltPair;
import suite.util.To;

/**
 * Eliminate price sparks caused by data source bugs.
 * 
 * @author ywsing
 */
public class Cleanse {

	public void cleanse(float[] prices) {
		cleanse(prices.length, i -> prices[i], (i, price) -> prices[i] = price);
	}

	public void cleanse(LngFltPair[] pairs) {
		cleanse(pairs.length, i -> pairs[i].t1, (i, price) -> pairs[i].t1 = price);
	}

	public boolean isValid(float price0, float price1) {
		return isValid_(price0, price1);
	}

	private void cleanse(int length, Int_Flt get, IntFlt_Flt set) {

		// fill zeroes in the middle
		var ppos = 0f;
		var i0 = 0;
		for (var i = 0; i < length; i++) {
			var price = get.apply(i);
			if (price != 0f) {
				while (i0 < i)
					set.apply(i0++, ppos);
				ppos = price;
			}
		}

		// eliminate spikes
		for (var i = 2; i < length; i++) {
			var price0 = get.apply(i - 2);
			var price1 = get.apply(i - 1);
			var price2 = get.apply(i - 0);
			if (isValid(price0, price2) && !isValid(price0, price1) && !isValid(price1, price2)) {
				set.apply(i - 1, price0);
				if (Boolean.FALSE)
					Log_.warn("price spiked: " + To.string(price0) + ", " + To.string(price1) + ", " + To.string(price2));
			}
		}
	}

	private boolean isValid_(float price0, float price1) {
		var ratio = price1 / price0;
		return Float.isFinite(ratio) && 1f / 2f < ratio && ratio < 2f / 1f;
	}

}
