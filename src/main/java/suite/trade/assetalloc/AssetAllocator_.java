package suite.trade.assetalloc;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import suite.adt.Pair;
import suite.streamlet.As;
import suite.streamlet.Read;
import suite.trade.Asset;
import suite.trade.MovingAverage;
import suite.trade.data.DataSource;
import suite.util.String_;

public class AssetAllocator_ {

	public static AssetAllocator byEma() {
		MovingAverage movingAvg = new MovingAverage();
		int halfLife = 64;
		double decay = Math.exp(Math.log(.5d) / halfLife);
		double threshold = .9d;

		return new AssetAllocator() {
			public List<Pair<String, Double>> allocate( //
					Map<String, DataSource> dataSourceBySymbol, //
					List<LocalDate> tradeDates, //
					LocalDate backTestDate) {
				return Read.from2(dataSourceBySymbol) //
						.map2((symbol, dataSource) -> {
							float[] ema = movingAvg.exponentialMovingAvg(dataSource.prices, decay);
							float lastEma = ema[ema.length - 2];
							float latest = dataSource.last().price;
							return latest / lastEma < threshold ? 1d : 0d;
						}) //
						.toList();
			}
		};
	}

	public static AssetAllocator byLastPriceChange() {
		return new AssetAllocator() {
			public List<Pair<String, Double>> allocate( //
					Map<String, DataSource> dataSourceBySymbol, //
					List<LocalDate> tradeDates, //
					LocalDate backTestDate) {
				return Read.from2(dataSourceBySymbol) //
						.map2((symbol, dataSource) -> dataSource.get(-2).price / dataSource.get(-1).price < .96d ? 1d : 0d) //
						.toList();
			}
		};
	}

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

	public static AssetAllocator byWorstReturn() {
		return new AssetAllocator() {
			public List<Pair<String, Double>> allocate( //
					Map<String, DataSource> dataSourceBySymbol, //
					List<LocalDate> tradeDates, //
					LocalDate backTestDate) {
				return Read.from2(dataSourceBySymbol) //
						.map2((symbol, dataSource) -> {
							float price0 = dataSource.get(-2).price;
							float price1 = dataSource.get(-1).price;
							return price1 / price0 - 1f;
						}) //
						.sortBy((symbol, return_) -> return_) //
						.take(1) //
						.mapValue(return_ -> 1d) //
						.toList();
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

	public static AssetAllocator removeShorts(AssetAllocator assetAllocator) {
		return (dataSourceBySymbol, tradeDates, backTestDate) -> {
			List<Pair<String, Double>> potentialBySymbol = assetAllocator.allocate(dataSourceBySymbol, tradeDates, backTestDate);
			return Read.from2(potentialBySymbol).filterValue(potential -> 0d < potential).toList();
		};
	}

}
