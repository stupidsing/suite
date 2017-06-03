package suite.trade.assetalloc;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Predicate;

import suite.adt.pair.Pair;
import suite.math.stat.BollingerBands;
import suite.math.stat.Statistic;
import suite.math.stat.Statistic.MeanVariance;
import suite.math.stat.TimeSeries;
import suite.math.stat.TimeSeries.Donchian;
import suite.streamlet.As;
import suite.streamlet.Read;
import suite.streamlet.Streamlet2;
import suite.trade.Asset;
import suite.trade.MovingAverage;
import suite.trade.assetalloc.AssetAllocator.OnDate;
import suite.trade.data.Configuration;
import suite.trade.data.DataSource;
import suite.trade.singlealloc.BuySellStrategy;
import suite.trade.singlealloc.BuySellStrategy.GetBuySell;
import suite.trade.singlealloc.Strategos;
import suite.util.String_;

public class AssetAllocator_ {

	private static BollingerBands bb = new BollingerBands();
	private static MovingAverage ma = new MovingAverage();
	private static Statistic stat = new Statistic();
	private static TimeSeries ts = new TimeSeries();

	public static AssetAllocator bollingerBands() {
		return bollingerBands_(32, 0, 2);
	}

	public static AssetAllocator bollingerBands1() {
		return bollingerBands_(32 + 1, 1, 2);
	}

	public static AssetAllocator byEma() {
		int halfLife = 64;
		double decay = Math.exp(Math.log(.5d) / halfLife);
		double threshold = .9d;

		return (dataSourceBySymbol, dates) -> {
			Map<String, float[]> ema = dataSourceBySymbol //
					.mapValue(dataSource -> ma.exponentialMovingAvg(dataSource.prices, decay)) //
					.toMap();

			return (backTestDate, index) -> dataSourceBySymbol //
					.map2((symbol, dataSource) -> {
						int last = index - 1;
						float lastEma = ema.get(symbol)[last];
						float latest = dataSource.prices[last];
						return latest / lastEma < threshold ? 1d : 0d;
					}) //
					.toList();
		};
	}

	public static AssetAllocator byLastPriceChange() {
		return (dataSourceBySymbol, dates) -> (backTestDate, index) -> dataSourceBySymbol //
				.map2((symbol, dataSource) -> dataSource.get(index - 2).price / dataSource.get(index - 1).price < .96d ? 1d : 0d) //
				.toList();
	}

	public static AssetAllocator byPairs(Configuration cfg, String symbol0, String symbol1) {
		return AssetAllocator_.filterAssets( //
				symbol -> String_.equals(symbol, symbol1), //
				AssetAllocator_.relativeToIndex( //
						cfg, //
						symbol0, //
						rsi_(32, .3d, .7d)));
	}

	public static AssetAllocator byReturnsProRata() {
		return (dataSourceBySymbol, dates) -> (backTestDate, index) -> {
			Streamlet2<String, Double> returns = dataSourceBySymbol //
					.map2((symbol, dataSource) -> {
						double price0 = dataSource.prices[index - 2];
						double price1 = dataSource.prices[index - 1];
						return (price0 - price1) / price0;
					}) //
					.filterValue(return_ -> 0d < return_) //
					.collect(As::streamlet2);

			double sum = returns.collectAsDouble(As.sumOfDoubles((symbol, price) -> price));
			return returns.mapValue(return_ -> return_ / sum).toList();
		};
	}

	public static AssetAllocator byTradeFrequency(int tradeFrequency, AssetAllocator assetAllocator) {
		return (dataSourceBySymbol, dates) -> {
			OnDate onDate = assetAllocator.allocate(dataSourceBySymbol, dates);

			return new OnDate() {
				private LocalDate date0;
				private List<Pair<String, Double>> result0;

				public List<Pair<String, Double>> onDate(LocalDate backTestDate0, int index) {
					LocalDate backTestDate1 = backTestDate0.minusDays(backTestDate0.toEpochDay() % tradeFrequency);
					if (!Objects.equals(date0, backTestDate1)) {
						date0 = backTestDate1;
						return result0 = onDate.onDate(backTestDate1, index);
					} else
						return result0;
				}
			};
		};
	}

	public static AssetAllocator byWorstReturn() {
		return (dataSourceBySymbol, dates) -> (backTestDate, index) -> dataSourceBySymbol //
				.map2((symbol, dataSource) -> {
					float[] prices = dataSource.prices;
					float price0 = prices[index - 2];
					float price1 = prices[index - 1];
					return price1 / price0 - 1f;
				}) //
				.sortBy((symbol2, return_) -> return_) //
				.take(1) //
				.mapValue(return_ -> 1d) //
				.toList();
	}

	public static AssetAllocator donchian() {
		int window = 32;

		return AssetAllocator_.unleverage((dataSourceBySymbol, dates) -> {
			Map<String, Donchian> donchianBySymbol = dataSourceBySymbol //
					.mapValue(dataSource -> ts.donchian(window, dataSource.prices)) //
					.toMap();

			return (backTestDate, index) -> dataSourceBySymbol //
					.map2((symbol, dataSource) -> {
						Donchian donchian = donchianBySymbol.get(symbol);
						float price = dataSource.prices[index - 1];
						boolean hold = false;
						for (int i = 0; i < index; i++)
							if (price == donchian.mins[i])
								hold = true;
							else if (price == donchian.maxs[i])
								hold = false;
						return hold ? 1d : 0d;
					}) //
					.toList();
		});
	}

	public static AssetAllocator dump(AssetAllocator assetAllocator0) {
		return (dataSourceBySymbol, dates) -> {
			OnDate onDate = assetAllocator0.allocate(dataSourceBySymbol, dates);

			return (backTestDate, index) -> {
				List<Pair<String, Double>> ratioBySymbol = onDate.onDate(backTestDate, index);
				System.out.println("ratioBySymbol = " + ratioBySymbol);
				return ratioBySymbol;
			};
		};
	}

	public static AssetAllocator even(AssetAllocator assetAllocator0) {
		AssetAllocator assetAllocator1 = filterShorts_(assetAllocator0);

		return (dataSourceBySymbol, dates) -> {
			OnDate onDate = assetAllocator1.allocate(dataSourceBySymbol, dates);

			return (backTestDate, index) -> {
				List<Pair<String, Double>> potentialBySymbol = onDate.onDate(backTestDate, index);
				double each = 1d / Read.from2(potentialBySymbol).size();

				return Read.from2(potentialBySymbol) //
						.filterKey(symbol -> !String_.equals(symbol, Asset.cashSymbol)) //
						.mapValue(potential -> 1d / each) //
						.toList();
			};
		};
	}

	public static AssetAllocator filterAssets(Predicate<String> pred, AssetAllocator assetAllocator) {
		return (dataSourceBySymbol, dates) -> assetAllocator.allocate(dataSourceBySymbol.filterKey(pred), dates)::onDate;
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

		return AssetAllocator_.unleverage((dataSourceBySymbol, dates) -> {
			Map<String, GetBuySell> getBuySellBySymbol = dataSourceBySymbol //
					.mapValue(dataSource -> mamr.analyze(dataSource.prices)) //
					.toMap();

			return (backTestDate, index) -> dataSourceBySymbol //
					.map2((symbol, dataSource) -> {
						GetBuySell gbs = getBuySellBySymbol.get(symbol);
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
		return AssetAllocator_.unleverage((dataSourceBySymbol, dates) -> {
			Map<String, float[]> movingMedian0BySymbol = dataSourceBySymbol //
					.mapValue(dataSource -> ma.movingMedian(dataSource.prices, windowSize0)) //
					.toMap();

			Map<String, float[]> movingMedian1BySymbol = dataSourceBySymbol //
					.mapValue(dataSource -> ma.movingMedian(dataSource.prices, windowSize1)) //
					.toMap();

			return (backTestDate, index) -> dataSourceBySymbol //
					.map2((symbol, dataSource) -> {
						float[] movingMedian0 = movingMedian0BySymbol.get(symbol);
						float[] movingMedian1 = movingMedian1BySymbol.get(symbol);
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
		return (dataSourceBySymbol, dates) -> (backTestDate, index) -> Arrays.asList(Pair.of(symbol, 1d));
	}

	public static AssetAllocator questoQuella(String symbol0, String symbol1) {
		int tor = 64;
		double threshold = 0d;

		return AssetAllocator_.filterAssets( //
				symbol -> String_.equals(symbol, symbol0) || String_.equals(symbol, symbol1), //
				(dataSourceBySymbol, dates) -> {
					Map<String, DataSource> dataSources = dataSourceBySymbol.toMap();
					DataSource dataSource0 = dataSources.get(symbol0);
					DataSource dataSource1 = dataSources.get(symbol1);

					return (backTestDate, index) -> {
						int ix = index - 1;
						int i0 = ix - tor;
						double p0 = dataSource0.get(i0).price, px = dataSource0.get(ix).price;
						double q0 = dataSource1.get(i0).price, qx = dataSource1.get(ix).price;
						double pdiff = (px - p0) / px;
						double qdiff = (qx - q0) / qx;

						if (threshold < Math.abs(pdiff - qdiff))
							return Arrays.asList(Pair.of(pdiff < qdiff ? symbol0 : symbol1, 1d));
						else
							return Collections.emptyList();
					};
				});
	}

	public static AssetAllocator reallocate(AssetAllocator assetAllocator) {
		return (dataSourceBySymbol, dates) -> {
			OnDate onDate = assetAllocator.allocate(dataSourceBySymbol, dates);

			return (backTestDate, index) -> {
				List<Pair<String, Double>> potentialBySymbol = onDate.onDate(backTestDate, index);
				return scale(potentialBySymbol, 1d / totalPotential(potentialBySymbol));
			};
		};
	}

	public static AssetAllocator relative(AssetAllocator assetAllocator, DataSource index) {
		return relative_(assetAllocator, index);
	}

	public static AssetAllocator relativeToHsi(Configuration cfg, AssetAllocator assetAllocator) {
		return relativeToIndex(cfg, "^HSI", assetAllocator);
	}

	public static AssetAllocator relativeToIndex(Configuration cfg, String indexSymbol, AssetAllocator assetAllocator) {
		return relative_(assetAllocator, cfg.dataSourceWithLatestQuote(indexSymbol));
	}

	public static AssetAllocator rsi() {
		return rsi_(32, .3d, .7d);
	}

	public static AssetAllocator unleverage(AssetAllocator assetAllocator0) {
		return unleverage_(assetAllocator0);
	}

	public static AssetAllocator variableBollingerBands() {
		return AssetAllocator_.unleverage((dataSourceBySymbol, dates) -> {
			return (backTestDate, index) -> dataSourceBySymbol //
					.map2((symbol, dataSource) -> {
						int last = index - 1;
						double hold = 0d;

						for (int window = 1; hold == 0d && window < 256; window++) {
							float price = dataSource.prices[last];
							MeanVariance mv = stat.meanVariance(Arrays.copyOfRange(dataSource.prices, last - window, last));
							double mean = mv.mean;
							double diff = 3d * mv.standardDeviation();

							if (price < mean - diff)
								hold = 1d;
							else if (mean + diff < price)
								hold = -1d;
						}

						return hold;
					}) //
					.toList();
		});
	}

	public static AssetAllocator threeMovingAvgs() {
		return AssetAllocator_.unleverage((dataSourceBySymbol, dates) -> {
			Map<String, float[]> movingAvg0BySymbol = dataSourceBySymbol //
					.mapValue(dataSource -> ma.exponentialMovingGeometricAvg(dataSource.prices, .11d)) //
					.toMap();

			Map<String, float[]> movingAvg1BySymbol = dataSourceBySymbol //
					.mapValue(dataSource -> ma.exponentialMovingGeometricAvg(dataSource.prices, .08d)) //
					.toMap();

			Map<String, float[]> movingAvg2BySymbol = dataSourceBySymbol //
					.mapValue(dataSource -> ma.exponentialMovingGeometricAvg(dataSource.prices, .05d)) //
					.toMap();

			return (backTestDate, index) -> dataSourceBySymbol //
					.map2((symbol, dataSource) -> {
						int last = index - 1;
						float movingAvg0 = movingAvg0BySymbol.get(symbol)[last];
						float movingAvg1 = movingAvg1BySymbol.get(symbol)[last];
						float movingAvg2 = movingAvg2BySymbol.get(symbol)[last];
						if (movingAvg0 < movingAvg1 && movingAvg1 < movingAvg2)
							return -1d;
						else if (movingAvg2 < movingAvg1 && movingAvg1 < movingAvg0)
							return 1d;
						else
							return 0d;
					}) //
					.toList();
		});
	}

	private static double totalPotential(List<Pair<String, Double>> potentialBySymbol) {
		return Read.from2(potentialBySymbol).collectAsDouble(As.sumOfDoubles((symbol, potential) -> potential));
	}

	private static AssetAllocator bollingerBands_(int backPos0, int backPos1, int k) {
		return AssetAllocator_.unleverage((dataSourceBySymbol, dates) -> {
			Map<String, float[]> percentbBySymbol = dataSourceBySymbol //
					.mapValue(dataSource -> bb.bb(dataSource.prices, backPos0, backPos1, k).percentb) //
					.toMap();

			return (backTestDate, index) -> dataSourceBySymbol //
					.map2((symbol, dataSource) -> {
						float[] percentbs = percentbBySymbol.get(symbol);
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
					.toList();
		});
	}

	private static AssetAllocator relative_(AssetAllocator assetAllocator, DataSource indexDataSource) {
		return (dataSourceBySymbol0, dates_) -> {
			Streamlet2<String, DataSource> dataSourceBySymbol1 = dataSourceBySymbol0 //
					.mapValue(dataSource0 -> {
						String[] dates = dataSource0.dates;
						float[] prices = dataSource0.prices;
						String[] indexDates = indexDataSource.dates;
						float[] indexPrices = indexDataSource.prices;
						int length = dates.length;
						int indexLength = indexDates.length;
						float[] prices1 = new float[length];
						int ii = 0;

						for (int si = 0; si < length; si++) {
							String date = dates[si];
							while (ii < indexLength && indexDates[ii].compareTo(date) < 0)
								ii++;
							prices1[si] = prices[si] / indexPrices[ii];
						}

						return new DataSource(dates, prices1);
					}) //
					.collect(As::streamlet2);

			return assetAllocator.allocate(dataSourceBySymbol1, dates_)::onDate;
		};
	}

	private static AssetAllocator rsi_(int window, double threshold0, double threshold1) {
		return AssetAllocator_.unleverage((dataSourceBySymbol, dates) -> (backTestDate, index) -> dataSourceBySymbol //
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

	private static AssetAllocator unleverage_(AssetAllocator assetAllocator0) {
		AssetAllocator assetAllocator1 = filterShorts_(assetAllocator0);

		return (dataSourceBySymbol, dates) -> {
			OnDate onDate = assetAllocator1.allocate(dataSourceBySymbol, dates);

			return (backTestDate, index) -> {
				List<Pair<String, Double>> potentialBySymbol = onDate.onDate(backTestDate, index);
				double totalPotential = totalPotential(potentialBySymbol);
				if (1d < totalPotential)
					return scale(potentialBySymbol, 1d / totalPotential);
				else
					return potentialBySymbol;
			};
		};
	}

	private static AssetAllocator filterShorts_(AssetAllocator assetAllocator) {
		return (dataSourceBySymbol, dates) -> {
			OnDate onDate = assetAllocator.allocate(dataSourceBySymbol, dates);

			return (backTestDate, index) -> {
				List<Pair<String, Double>> potentialBySymbol = onDate.onDate(backTestDate, index);

				return Read.from2(potentialBySymbol) //
						.map2(AssetAllocator_::validate) //
						.filterValue(potential -> 0d < potential) //
						.toList();
			};
		};
	}

	private static Double validate(String symbol, Double potential) {
		if (Double.isFinite(potential))
			return potential;
		else
			throw new RuntimeException("potential is " + potential);
	}

}
