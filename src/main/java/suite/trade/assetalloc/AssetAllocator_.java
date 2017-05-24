package suite.trade.assetalloc;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Predicate;

import suite.adt.pair.Pair;
import suite.math.stat.BollingerBands;
import suite.math.stat.TimeSeries;
import suite.math.stat.TimeSeries.Donchian;
import suite.streamlet.As;
import suite.streamlet.Read;
import suite.trade.Asset;
import suite.trade.MovingAverage;
import suite.trade.data.Configuration;
import suite.trade.data.DataSource;
import suite.trade.singlealloc.BuySellStrategy;
import suite.trade.singlealloc.BuySellStrategy.GetBuySell;
import suite.trade.singlealloc.Strategos;
import suite.util.String_;

public class AssetAllocator_ {

	private static BollingerBands bb = new BollingerBands();
	private static MovingAverage ma = new MovingAverage();
	private static TimeSeries ts = new TimeSeries();

	public static AssetAllocator bollingerBands() {
		int window = 32;
		int k = 2;

		return AssetAllocator_.unleverage((dataSourceBySymbol, backTestDate, index) -> Read //
				.from2(dataSourceBySymbol) //
				.mapValue(dataSource -> {
					float[] percentbs = bb.bb(dataSource.prices, window, k).percentb;
					double hold = 0d;
					for (int i = 0; i < index; i++) {
						float percentb = percentbs[i];
						if (percentb <= 0f)
							hold = 1d;
						else if (.5f < percentb) // un-short
							hold = 0d <= hold ? hold : 0d;
						else if (percentb < 1f) // un-long
							hold = hold < 0d ? hold : 0d;
						else
							hold = -1d;
					}
					return hold;
				}) //
				.toList());
	}

	public static AssetAllocator byEma() {
		int halfLife = 64;
		double decay = Math.exp(Math.log(.5d) / halfLife);
		double threshold = .9d;

		return (dataSourceBySymbol, backTestDate, index) -> Read //
				.from2(dataSourceBySymbol) //
				.map2((symbol, dataSource) -> {
					float[] ema = ma.exponentialMovingAvg(dataSource.prices, decay);
					int last = index - 1;
					float lastEma = ema[last];
					float latest = dataSource.prices[last];
					return latest / lastEma < threshold ? 1d : 0d;
				}) //
				.toList();
	}

	public static AssetAllocator byLastPriceChange() {
		return (dataSourceBySymbol, backTestDate, index) -> Read //
				.from2(dataSourceBySymbol) //
				.map2((symbol, dataSource) -> dataSource.get(index - 2).price / dataSource.get(index - 1).price < .96d ? 1d : 0d) //
				.toList();
	}

	public static AssetAllocator byPairs(Configuration cfg, Asset asset0, Asset asset1) {
		return AssetAllocator_.filterAssets( //
				symbol -> String_.equals(symbol, asset1.symbol), //
				AssetAllocator_.relativeToIndex( //
						cfg, //
						asset0.symbol, //
						rsi_(32, .3d, .7d)));
	}

	public static AssetAllocator byTradeFrequency(int tradeFrequency, AssetAllocator assetAllocator) {
		return new AssetAllocator() {
			private LocalDate date0;
			private List<Pair<String, Double>> result0;

			public List<Pair<String, Double>> allocate(Map<String, DataSource> dataSourceBySymbol, LocalDate backTestDate0,
					int index) {
				LocalDate backTestDate1 = backTestDate0.minusDays(backTestDate0.toEpochDay() % tradeFrequency);
				if (!Objects.equals(date0, backTestDate1)) {
					date0 = backTestDate1;
					return result0 = assetAllocator.allocate(dataSourceBySymbol, backTestDate1, index);
				} else
					return result0;
			}
		};
	}

	public static AssetAllocator byWorstReturn() {
		return (dataSourceBySymbol, backTestDate, index) -> Read //
				.from2(dataSourceBySymbol) //
				.map2((symbol1, dataSource) -> {
					float[] prices = dataSource.prices;
					float price0 = prices[index - 2];
					float price1 = prices[index - 1];
					return price1 / price0 - 1f;
				}) //
				.sortBy((symbol2, return_1) -> return_1) //
				.take(1) //
				.mapValue(return_2 -> 1d) //
				.toList();
	}

	public static AssetAllocator donchian() {
		int window = 32;

		return AssetAllocator_.unleverage((dataSourceBySymbol, backTestDate, index) -> Read //
				.from2(dataSourceBySymbol) //
				.mapValue(dataSource -> {
					float[] prices = dataSource.prices;
					Donchian donchian = ts.donchian(window, prices);
					float price = prices[index - 1];
					boolean hold = false;
					for (int i = 0; i < index; i++)
						if (price == donchian.mins[i])
							hold = true;
						else if (price == donchian.maxs[i])
							hold = false;
					return hold ? 1d : 0d;
				}) //
				.toList());
	}

	public static AssetAllocator dump(AssetAllocator assetAllocator0) {
		return (dataSourceBySymbol, backTestDate, index) -> {
			List<Pair<String, Double>> ratioBySymbol = assetAllocator0.allocate(dataSourceBySymbol, backTestDate, index);
			System.out.println("ratioBySymbol = " + ratioBySymbol);
			return ratioBySymbol;
		};
	}

	public static AssetAllocator even(AssetAllocator assetAllocator0) {
		AssetAllocator assetAllocator1 = filterShorts_(assetAllocator0);
		return (dataSourceBySymbol, backTestDate, index) -> {
			List<Pair<String, Double>> potentialBySymbol = assetAllocator1.allocate(dataSourceBySymbol, backTestDate, index);
			double each = 1d / Read.from2(potentialBySymbol).size();

			return Read.from2(potentialBySymbol) //
					.filterKey(symbol -> !String_.equals(symbol, Asset.cashCode)) //
					.mapValue(potential -> 1d / each) //
					.toList();
		};
	}

	public static AssetAllocator filterAssets(Predicate<String> pred, AssetAllocator assetAllocator) {
		return (dataSourceBySymbol, backTestDate, index) -> assetAllocator //
				.allocate(Read.from2(dataSourceBySymbol).filterKey(pred).toMap(), backTestDate, index);
	}

	public static AssetAllocator filterShorts(AssetAllocator assetAllocator) {
		return filterShorts_(assetAllocator);
	}

	public static AssetAllocator movingAvg() {
		int nPastDays = 64;
		int nHoldDays = 8;
		float threshold = .15f;
		Strategos strategos = new Strategos();
		BuySellStrategy mamr = strategos.movingAvgMeanReverting(nPastDays, nHoldDays, threshold);

		return AssetAllocator_.unleverage((dataSourceBySymbol, backTestDate, index) -> {
			return Read.from2(dataSourceBySymbol) //
					.mapValue(dataSource -> {
						float[] prices = dataSource.prices;
						GetBuySell gbs = mamr.analyze(prices);
						int hold = 0;

						for (int i = 0; i < index; i++)
							hold += gbs.get(i);

						return (double) hold;
					}) //
					.toList();
		});
	}

	public static AssetAllocator movingMedianMeanReversion() {
		int windowSize0 = 1;
		int windowSize1 = 32;
		return AssetAllocator_.unleverage((dataSourceBySymbol, backTestDate, index) -> {
			return Read.from2(dataSourceBySymbol) //
					.mapValue(dataSource -> {
						float[] prices = dataSource.prices;
						float[] movingMedian0 = ma.movingMedian(prices, windowSize0);
						float[] movingMedian1 = ma.movingMedian(prices, windowSize1);
						int last = index - 1;
						double median0 = movingMedian0[last];
						double median1 = movingMedian1[last];
						double ratio = median1 / median0;
						return ratio - 1d;
					}) //
					.toList();
		});
	}

	public static AssetAllocator ofSingle(String symbol) {
		return (dataSourceBySymbol, backTestDate, index) -> Arrays.asList(Pair.of(symbol, 1d));
	}

	public static AssetAllocator reallocate(AssetAllocator assetAllocator) {
		return (dataSourceBySymbol, backTestDate, index) -> {
			List<Pair<String, Double>> potentialBySymbol = assetAllocator.allocate(dataSourceBySymbol, backTestDate, index);
			return scale(potentialBySymbol, 1d / totalPotential(potentialBySymbol));
		};
	}

	public static AssetAllocator relative(AssetAllocator assetAllocator, DataSource index) {
		return relative_(assetAllocator, index);
	}

	public static AssetAllocator relativeToIndex(Configuration cfg, String indexSymbol, AssetAllocator assetAllocator) {
		return relative_(assetAllocator, cfg.dataSourceWithLatestQuote(indexSymbol));
	}

	public static AssetAllocator rsi() {
		return rsi_(32, .3d, .7d);
	}

	public static AssetAllocator unleverage(AssetAllocator assetAllocator0) {
		AssetAllocator assetAllocator1 = filterShorts_(assetAllocator0);
		return (dataSourceBySymbol, backTestDate, index) -> {
			List<Pair<String, Double>> potentialBySymbol = assetAllocator1.allocate(dataSourceBySymbol, backTestDate, index);
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
		return (dataSourceBySymbol, backTestDate, index) -> {
			List<Pair<String, Double>> potentialBySymbol = assetAllocator.allocate(dataSourceBySymbol, backTestDate, index);
			return Read.from2(potentialBySymbol) //
					.map2(AssetAllocator_::validate) //
					.filterValue(potential -> 0d < potential) //
					.toList();
		};
	}

	private static AssetAllocator relative_(AssetAllocator assetAllocator, DataSource indexDataSource) {
		return (dataSourceBySymbol0, backTestDate, index) -> {
			Map<String, DataSource> dataSourceBySymbol1 = Read.from2(dataSourceBySymbol0) //
					.mapValue(dataSource0 -> {
						String[] dates = dataSource0.dates;
						String[] indexDates = indexDataSource.dates;
						float[] prices = dataSource0.prices;
						float[] indexPrices = indexDataSource.prices;
						int length = dates.length;
						int indexLength = indexDates.length;
						float[] prices1 = new float[length];
						int ii = 0;
						for (int si = 0; si < index; si++) {
							String date = dates[si];
							while (ii < indexLength && indexDates[ii].compareTo(date) < 0)
								ii++;
							prices1[si] = prices[si] / indexPrices[ii];
						}
						return new DataSource(dates, prices1);
					}) //
					.toMap();

			return assetAllocator.allocate(dataSourceBySymbol1, backTestDate, index);
		};
	}

	private static AssetAllocator rsi_(int window, double threshold0, double threshold1) {
		return AssetAllocator_.unleverage((dataSourceBySymbol, backTestDate, index) -> Read //
				.from2(dataSourceBySymbol) //
				.mapValue(dataSource -> {
					float[] prices = dataSource.prices;
					int u = 0;
					for (int i = index - window; i < index; i++)
						if (prices[i - 1] < prices[i])
							u++;
					double rsi = (double) u / window;
					if (rsi < threshold0) // over-sold
						return .5d - rsi;
					else if (threshold1 < rsi) // over-bought
						return .5d - rsi;
					else
						return 0d;
				}) //
				.toList());
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
