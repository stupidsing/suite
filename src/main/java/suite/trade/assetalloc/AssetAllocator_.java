package suite.trade.assetalloc;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import suite.adt.Pair;
import suite.streamlet.As;
import suite.streamlet.Read;
import suite.trade.Asset;
import suite.trade.data.DataSource;
import suite.util.String_;

public class AssetAllocator_ {

	public static AssetAllocator byTradeFrequency(AssetAllocator assetAllocator, int tradeFrequency) {
		return new AssetAllocator() {
			private LocalDate date0;
			private List<Pair<String, Double>> result0;

			public List<Pair<String, Double>> allocate( //
					Map<String, DataSource> dataSourceBySymbol, //
					List<LocalDate> tradeDates, //
					LocalDate backTestDate0) {
				LocalDate backTestDate1 = backTestDate0.minusDays(backTestDate0.toEpochDay() % tradeFrequency);
				if (!Objects.equals(date0, backTestDate1)) {
					date0 = backTestDate1;
					return result0 = assetAllocator.allocate(dataSourceBySymbol, tradeDates, backTestDate1);
				} else
					return result0;
			}
		};
	}

	public static AssetAllocator even(AssetAllocator assetAllocator) {
		return (dataSourceBySymbol, tradeDates, backTestDate) -> {
			List<Pair<String, Double>> potentialBySymbol = assetAllocator.allocate(dataSourceBySymbol, tradeDates, backTestDate);
			double each = 1d / Read.from2(potentialBySymbol).size();

			return Read.from2(potentialBySymbol) //
					.filterKey(symbol -> !String_.equals(symbol, Asset.cashCode)) //
					.mapValue(potential -> 1d / each) //
					.toList();
		};
	}

	public static AssetAllocator reallocate(AssetAllocator assetAllocator) {
		return (dataSourceBySymbol, tradeDates, backTestDate) -> {
			List<Pair<String, Double>> potentialBySymbol = assetAllocator.allocate(dataSourceBySymbol, tradeDates, backTestDate);
			double totalPotential = Read.from2(potentialBySymbol)
					.collectAsDouble(As.sumOfDoubles((symbol, potential) -> potential));

			return Read.from2(potentialBySymbol) //
					.filterKey(symbol -> !String_.equals(symbol, Asset.cashCode)) //
					.mapValue(potential -> potential / totalPotential) //
					.toList();
		};
	}

}
