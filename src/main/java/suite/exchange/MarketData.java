package suite.exchange;

import static java.lang.Math.max;
import static java.lang.Math.min;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import suite.trade.data.DataSource;

class MarketData {

	public final Map<Long, Data> dataByResolution = Collections.synchronizedMap(new HashMap<>());

	public class Data {
		public int length = 0;

		public DataSource ds = DataSource.ofOhlcv( //
				new long[128], //
				new float[128], //
				new float[128], //
				new float[128], //
				new float[128], //
				new float[128]);
	}

	public void update(long now, float mid, float volume) {
		for (var resolution : new long[] { 60 * 1000l, }) {
			var data = dataByResolution.computeIfAbsent(resolution, r -> new Data());
			var ds = data.ds;
			var lm1 = data.length - 1;
			var bar_ = now - now % resolution;
			var bar0 = 0 < data.length ? ds.ts[lm1] : bar_ - resolution;

			while (bar0 != bar_) {
				var l = data.length++;

				while (ds.ts.length <= l) {
					var expand = ds.ts.length * 2;

					ds = data.ds = DataSource.ofOhlcv( //
							Arrays.copyOf(ds.ts, expand), //
							Arrays.copyOf(ds.opens, expand), //
							Arrays.copyOf(ds.closes, expand), //
							Arrays.copyOf(ds.lows, expand), //
							Arrays.copyOf(ds.highs, expand), //
							Arrays.copyOf(ds.volumes, expand));
				}

				bar0 += resolution;
				ds.ts[l] = bar0;
				ds.prices[l] = Float.NaN;
				ds.opens[l] = Float.NaN;
				ds.closes[l] = Float.NaN;
				ds.lows[l] = Float.MAX_VALUE;
				ds.highs[l] = Float.MIN_VALUE;
				ds.volumes[l] = 0;
			}

			var open0 = ds.opens[lm1 = data.length - 1];

			ds.prices[lm1] = mid;
			ds.opens[lm1] = !Float.isNaN(open0) ? open0 : mid;
			ds.closes[lm1] = mid;
			ds.lows[lm1] = min(mid, ds.lows[lm1]);
			ds.highs[lm1] = max(mid, ds.highs[lm1]);
			ds.volumes[lm1] += volume;
		}
	}

}
