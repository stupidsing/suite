package suite.trade.assetalloc;

import java.util.List;

import suite.adt.Pair;
import suite.streamlet.As;
import suite.streamlet.Read;
import suite.trade.Asset;
import suite.util.String_;

public class AssetAllocator_ {

	public static AssetAllocator byTradeFrequency(AssetAllocator assetAllocator, int tradeFrequency) {
		return (dataSourceBySymbol, tradeDates, backTestDate) -> {
			if (backTestDate.toEpochDay() % tradeFrequency == 0)
				return assetAllocator.allocate(dataSourceBySymbol, tradeDates, backTestDate);
			else
				return null;
		};
	}

	public static AssetAllocator even(AssetAllocator assetAllocator) {
		return (dataSourceBySymbol, tradeDates, backTestDate) -> {
			List<Pair<String, Double>> potentialBySymbol = assetAllocator.allocate(dataSourceBySymbol, tradeDates, backTestDate);

			if (potentialBySymbol != null) {
				double each = 1d / Read.from2(potentialBySymbol).size();

				return Read.from2(potentialBySymbol) //
						.filterKey(symbol -> !String_.equals(symbol, Asset.cashCode)) //
						.mapValue(potential -> 1d / each) //
						.toList();
			} else
				return null;
		};
	}

	public static AssetAllocator reallocate(AssetAllocator assetAllocator) {
		return (dataSourceBySymbol, tradeDates, backTestDate) -> {
			List<Pair<String, Double>> potentialBySymbol = assetAllocator.allocate(dataSourceBySymbol, tradeDates, backTestDate);

			if (potentialBySymbol != null) {
				double totalPotential = Read.from2(potentialBySymbol) //
						.collectAsDouble(As.<String, Double> sumOfDoubles((symbol, potential) -> potential));

				return Read.from2(potentialBySymbol) //
						.filterKey(symbol -> !String_.equals(symbol, Asset.cashCode)) //
						.mapValue(potential -> potential / totalPotential) //
						.toList();
			} else
				return null;
		};
	}

}
