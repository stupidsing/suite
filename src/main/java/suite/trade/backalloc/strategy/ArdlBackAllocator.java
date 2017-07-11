package suite.trade.backalloc.strategy;

import java.util.Arrays;
import java.util.Collections;
import java.util.Map;

import suite.adt.pair.Pair;
import suite.math.stat.Ardl;
import suite.math.stat.Statistic.LinearRegression;
import suite.streamlet.Read;
import suite.streamlet.Streamlet2;
import suite.trade.backalloc.BackAllocator;
import suite.trade.data.DataSource;
import suite.trade.data.DataSource.AlignKeyDataSource;
import suite.util.To;

public class ArdlBackAllocator implements BackAllocator {

	private Ardl ardl = new Ardl(9, false);

	@Override
	public OnDateTime allocate(AlignKeyDataSource<String> akds, int[] indices) {
		Streamlet2<String, DataSource> dsBySymbol0 = akds.dsByKey;
		Map<String, DataSource> dsBySymbol1 = dsBySymbol0.toMap();
		String[] symbols = dsBySymbol0.keys().toArray(String.class);

		float[][] fs = Read.from(symbols) //
				.map(symbol -> dsBySymbol1.get(symbol).prices) //
				.toArray(float[].class);

		LinearRegression[] lrs = ardl.ardl(fs);

		return (time, index) -> {
			float[] prices = ardl.predict(lrs, fs, index);
			float[] returns = To.arrayOfFloats(prices.length, i -> prices[i] / fs[i][index]);
			float maxReturns = 0f;
			Integer maxi = null;

			for (int i = 0; i < returns.length; i++) {
				float return_ = returns[i];
				if (maxReturns < return_) {
					maxReturns = return_;
					maxi = i;
				}
			}

			return maxi != null ? Arrays.asList(Pair.of(symbols[maxi], 1d)) : Collections.emptyList();
		};
	}

}
