package suite.trade.backalloc.strategy;

import java.util.List;

import primal.adt.Pair;
import primal.fp.Funs.Fun;
import primal.primitive.Int_Dbl;
import primal.primitive.fp.AsDbl;
import suite.streamlet.Read;
import suite.trade.backalloc.BackAllocator;
import suite.trade.data.DataSource;
import suite.trade.singlealloc.BuySellStrategy;

public class BackAllocator_ {

	public static BackAllocator by(BuySellStrategy mamr) {
		return byPrices(prices -> {
			var getBuySell = mamr.analyze(prices);

			return index -> {
				var hold = 0;
				for (var i = 0; i < index; i++)
					hold += getBuySell.get(i);
				return (double) hold;
			};
		});
	}

	public static BackAllocator byDataSource(Fun<DataSource, Int_Dbl> fun) {
		return byDataSource_(fun);
	}

	public static BackAllocator byPrices(Fun<float[], Int_Dbl> fun) {
		return byDataSource_(ds -> fun.apply(ds.prices));
	}

	public static BackAllocator ofSingle(String symbol) {
		return (akds, indices) -> index -> List.of(Pair.of(symbol, 1d));
	}

	public static BackAllocator sum(BackAllocator... bas) {
		return (akds, indices) -> {
			var odts = Read //
					.from(bas) //
					.map(ba -> ba.allocate(akds, indices)) //
					.collect();

			return index -> odts //
					.flatMap(odt -> odt.onDateTime(index)) //
					.groupBy(Pair::fst, st -> st.toDouble(AsDbl.sum(Pair::snd))) //
					.toList();
		};
	}

	private static BackAllocator byDataSource_(Fun<DataSource, Int_Dbl> fun) {
		return (akds, indices) -> {
			var allocBySymbol = akds.dsByKey.mapValue(fun).collect();

			return index -> allocBySymbol.mapValue(alloc -> alloc.apply(index)).toList();
		};
	}

}
