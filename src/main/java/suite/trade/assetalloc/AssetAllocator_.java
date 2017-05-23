package suite.trade.assetalloc;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Predicate;

import suite.adt.pair.Pair;
import suite.streamlet.As;
import suite.streamlet.Read;
import suite.trade.Asset;
import suite.trade.MovingAverage;
import suite.trade.data.Configuration;
import suite.trade.data.DataSource;
import suite.util.String_;

public class AssetAllocator_ {

	public static AssetAllocator byEma() {
		MovingAverage movingAvg = new MovingAverage();
		int halfLife = 64;
		double decay = Math.exp(Math.log(.5d) / halfLife);
		double threshold = .9d;

		return (dataSourceBySymbol, backTestDate) -> Read.from2(dataSourceBySymbol) //
				.map2((symbol, dataSource) -> {
					float[] ema = movingAvg.exponentialMovingAvg(dataSource.prices, decay);
					float lastEma = ema[ema.length - 2];
					float latest = dataSource.last().price;
					return latest / lastEma < threshold ? 1d : 0d;
				}) //
				.toList();
	}

	public static AssetAllocator byLastPriceChange() {
		return (dataSourceBySymbol, backTestDate) -> Read.from2(dataSourceBySymbol) //
				.map2((symbol, dataSource) -> dataSource.get(-2).price / dataSource.get(-1).price < .96d ? 1d : 0d) //
				.toList();
	}

	public static AssetAllocator byPairs(Configuration cfg, Asset asset0, Asset asset1) {
		return AssetAllocator_.filterAssets( //
				symbol -> String_.equals(symbol, asset1.symbol), //
				IndexRelativeAssetAllocator.of( //
						cfg, //
						asset0.symbol, //
						RsiAssetAllocator.of()));
	}

	public static AssetAllocator byTradeFrequency(AssetAllocator assetAllocator, int tradeFrequency) {
		return new AssetAllocator() {
			private LocalDate date0;
			private List<Pair<String, Double>> result0;

			public List<Pair<String, Double>> allocate(Map<String, DataSource> dataSourceBySymbol, LocalDate backTestDate0) {
				LocalDate backTestDate1 = backTestDate0.minusDays(backTestDate0.toEpochDay() % tradeFrequency);
				if (!Objects.equals(date0, backTestDate1)) {
					date0 = backTestDate1;
					return result0 = assetAllocator.allocate(dataSourceBySymbol, backTestDate1);
				} else
					return result0;
			}
		};
	}

	public static AssetAllocator byWorstReturn() {
		return (dataSourceBySymbol, backTestDate) -> Read.from2(dataSourceBySymbol) //
				.map2((symbol1, dataSource) -> {
					float price0 = dataSource.get(-2).price;
					float price1 = dataSource.get(-1).price;
					return price1 / price0 - 1f;
				}) //
				.sortBy((symbol2, return_1) -> return_1) //
				.take(1) //
				.mapValue(return_2 -> 1d) //
				.toList();
	}

	public static AssetAllocator even(AssetAllocator assetAllocator0) {
		AssetAllocator assetAllocator1 = filterShorts_(assetAllocator0);
		return (dataSourceBySymbol, backTestDate) -> {
			List<Pair<String, Double>> potentialBySymbol = assetAllocator1.allocate(dataSourceBySymbol, backTestDate);
			double each = 1d / Read.from2(potentialBySymbol).size();

			return Read.from2(potentialBySymbol) //
					.filterKey(symbol -> !String_.equals(symbol, Asset.cashCode)) //
					.mapValue(potential -> 1d / each) //
					.toList();
		};
	}

	public static AssetAllocator filterAssets(Predicate<String> pred, AssetAllocator assetAllocator) {
		return (dataSourceBySymbol, backTestDate) -> assetAllocator //
				.allocate(Read.from2(dataSourceBySymbol).filterKey(pred).toMap(), backTestDate);
	}

	public static AssetAllocator filterShorts(AssetAllocator assetAllocator) {
		return filterShorts_(assetAllocator);
	}

	public static AssetAllocator ofSingle(String symbol) {
		return (dataSourceBySymbol, backTestDate) -> Arrays.asList(Pair.of(symbol, 1d));
	}

	public static AssetAllocator reallocate(AssetAllocator assetAllocator) {
		return (dataSourceBySymbol, backTestDate) -> {
			List<Pair<String, Double>> potentialBySymbol = assetAllocator.allocate(dataSourceBySymbol, backTestDate);
			return scale(potentialBySymbol, 1d / totalPotential(potentialBySymbol));
		};
	}

	public static AssetAllocator unleverage(AssetAllocator assetAllocator0) {
		AssetAllocator assetAllocator1 = filterShorts_(assetAllocator0);
		return (dataSourceBySymbol, backTestDate) -> {
			List<Pair<String, Double>> potentialBySymbol = assetAllocator1.allocate(dataSourceBySymbol, backTestDate);
			double totalPotential = totalPotential(potentialBySymbol);
			if (1d < totalPotential)
				return scale(potentialBySymbol, 1d / totalPotential);
			else
				return potentialBySymbol;
		};
	}

	private static double totalPotential(List<Pair<String, Double>> potentialBySymbol) {
		return Read.from2(potentialBySymbol).collectAsDouble(As.sumOfDoubles((symbol, potential) -> potential));
	}

	private static AssetAllocator filterShorts_(AssetAllocator assetAllocator) {
		return (dataSourceBySymbol, backTestDate) -> {
			List<Pair<String, Double>> potentialBySymbol = assetAllocator.allocate(dataSourceBySymbol, backTestDate);
			return Read.from2(potentialBySymbol) //
					.map2(AssetAllocator_::validate) //
					.filterValue(potential -> 0d < potential) //
					.toList();
		};
	}

	private static List<Pair<String, Double>> scale(List<Pair<String, Double>> potentialBySymbol, double scale) {
		return Read.from2(potentialBySymbol) //
				.mapValue(potential -> potential * scale) //
				.toList();
	}

	private static Double validate(String symbol, Double potential) {
		if (Double.isFinite(potential))
			return potential;
		else
			throw new RuntimeException("potential is " + potential);
	}

}
