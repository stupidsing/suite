package suite.trade.backalloc.strategy;

import java.util.List;

import primal.MoreVerbs.Read;
import primal.adt.Pair;
import suite.trade.backalloc.BackAllocator;
import suite.trade.data.DataSource.AlignKeyDataSource;
import suite.ts.Ardl;
import suite.util.To;

public class ArdlBackAllocator implements BackAllocator {

	private Ardl ardl = new Ardl(9, false);

	@Override
	public OnDateTime allocate(AlignKeyDataSource<String> akds, int[] indices) {
		var dsBySymbol0 = akds.dsByKey;
		var dsBySymbol1 = dsBySymbol0.toMap();
		var symbols = dsBySymbol0.keys().toArray(String.class);

		var fs = Read
				.from(symbols)
				.map(symbol -> dsBySymbol1.get(symbol).prices)
				.toArray(float[].class);

		var lrs = ardl.ardl(fs);

		return index -> {
			var prices = ardl.predict(lrs, fs, index);
			var returns = To.vector(prices.length, i -> prices[i] / fs[i][index]);
			var maxReturns = 0f;
			Integer maxi = null;

			for (var i = 0; i < returns.length; i++) {
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
