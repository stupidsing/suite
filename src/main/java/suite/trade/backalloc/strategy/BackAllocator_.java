package suite.trade.backalloc.strategy;

import java.util.List;

import suite.adt.pair.Pair;
import suite.primitive.DblPrimitives.Obj_Dbl;
import suite.primitive.Int_Dbl;
import suite.streamlet.As;
import suite.streamlet.Read;
import suite.streamlet.Streamlet;
import suite.streamlet.Streamlet2;
import suite.trade.backalloc.BackAllocator;
import suite.trade.backalloc.BackAllocator.OnDateTime;
import suite.trade.data.DataSource;
import suite.trade.singlealloc.BuySellStrategy;
import suite.trade.singlealloc.BuySellStrategy.GetBuySell;
import suite.util.FunUtil.Fun;

public class BackAllocator_ {

	public static BackAllocator by(BuySellStrategy mamr) {
		return byPrices(prices -> {
			GetBuySell getBuySell = mamr.analyze(prices);

			return index -> {
				int hold = 0;
				for (int i = 0; i < index; i++)
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
			Streamlet<OnDateTime> odts = Read //
					.from(bas) //
					.map(ba -> ba.allocate(akds, indices)) //
					.collect(As::streamlet);

			return index -> odts //
					.flatMap(odt -> odt.onDateTime(index)) //
					.groupBy(Pair::first_, st -> st.toDouble(Obj_Dbl.sum(Pair::second))) //
					.toList();
		};
	}

	private static BackAllocator byDataSource_(Fun<DataSource, Int_Dbl> fun) {
		return (akds, indices) -> {
			Streamlet2<String, Int_Dbl> allocBySymbol = akds.dsByKey.mapValue(fun).collect(As::streamlet2);

			return index -> allocBySymbol.mapValue(alloc -> alloc.apply(index)).toList();
		};
	}

}
