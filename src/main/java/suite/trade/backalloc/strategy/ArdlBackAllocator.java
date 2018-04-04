package suite.trade.backalloc.strategy;

import java.util.List;
import java.util.Map;

import suite.adt.pair.Pair;
import suite.math.numeric.Statistic.LinearRegression;
import suite.primitive.Floats_;
import suite.streamlet.Read;
import suite.streamlet.Streamlet2;
import suite.trade.backalloc.BackAllocator;
import suite.trade.data.DataSource;
import suite.trade.data.DataSource.AlignKeyDataSource;
import ts.Ardl;

public class ArdlBackAllocator implements BackAllocator {

	private Ardl ardl = new Ardl(9, false);

	@Override
	public OnDateTime allocate(AlignKeyDataSource<String> akds, int[] indices) {
		Streamlet2<String, DataSource> dsBySymbol0 = akds.dsByKey;
		Map<String, DataSource> dsBySymbol1 = dsBySymbol0.toMap();
		String[] symbols = dsBySymbol0.keys().toArray(String.class);

		float[][] fs = Read //
				.from(symbols) //
				.map(symbol -> dsBySymbol1.get(symbol).prices) //
				.toArray(float[].class);

		LinearRegression[] lrs = ardl.ardl(fs);

		return index -> {
			float[] prices = ardl.predict(lrs, fs, index);
			float[] returns = Floats_.toArray(prices.length, i -> prices[i] / fs[i][index]);
			var maxReturns = 0f;
			Integer maxi = null;

			for (int i = 0; i < returns.length; i++) {
				var return_ = returns[i];
				if (maxReturns < return_) {
					maxReturns = return_;
					maxi = i;
				}
			}

			return maxi != null ? List.of(Pair.of(symbols[maxi], 1d)) : List.of();
		};
	}

}
