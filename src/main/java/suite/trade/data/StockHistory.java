package suite.trade.data;

import suite.adt.pair.LngFltPair;
import suite.trade.Time;
import suite.util.To;

public class StockHistory {

	public final LngFltPair[] prices0; // un-adjusted
	public final LngFltPair[] dividends;
	public final LngFltPair[] splits;

	public static StockHistory of(LngFltPair[] prices0, LngFltPair[] dividends, LngFltPair[] splits) {
		return new StockHistory(prices0, dividends, splits);
	}

	private StockHistory(LngFltPair[] prices0, LngFltPair[] dividends, LngFltPair[] splits) {
		this.prices0 = prices0;
		this.dividends = dividends;
		this.splits = splits;
	}

	public DataSource adjust() {
		int length = prices0.length;
		String[] dates = To.array(String.class, length, i -> Time.ofEpochUtcSecond(prices0[i].t0).ymd());
		float[] prices = To.arrayOfFloats(prices0, pair -> pair.t1);

		int di = dividends.length - 1;
		int si = splits.length - 1;
		float a = 0f, b = 1f;

		for (int i = length - 1; 0 <= i; i--) {
			prices[i] = a + b * prices[i];
			long epoch = prices0[i].t0;

			if (0 <= di) {
				LngFltPair dividend = dividends[di];
				if (epoch == dividend.t0) {
					a -= dividend.t1;
					di--;
				}
			}

			if (0 <= si) {
				LngFltPair split = splits[si];
				if (epoch == split.t0) {
					a *= split.t1;
					b *= split.t1;
					si--;
				}
			}
		}

		return new DataSource(dates, prices);
	}

}
