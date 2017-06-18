package suite.trade.backalloc;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

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
import suite.trade.backalloc.BackAllocator.OnDateTime;
import suite.trade.data.Configuration;
import suite.trade.data.DataSource;
import suite.trade.singlealloc.BuySellStrategy;
import suite.trade.singlealloc.BuySellStrategy.GetBuySell;
import suite.trade.singlealloc.Strategos;
import suite.util.String_;

public class BackAllocator_ {

	private static BollingerBands bb = new BollingerBands();
	private static MovingAverage ma = new MovingAverage();
	private static Statistic stat = new Statistic();
	private static TimeSeries ts = new TimeSeries();

	public static BackAllocator bollingerBands() {
		return bollingerBands_(2f);
	}

	public static BackAllocator bollingerBands(float k) {
		return bollingerBands_(k);
	}

	public static BackAllocator bollingerBands1() {
		int lag = 1; // lag 1 day is better in back tests, do not know why
		return bollingerBands_(32 + lag, lag, 2f);
	}

	public static BackAllocator byEma() {
		int halfLife = 64;
		double decay = Math.exp(Math.log(.5d) / halfLife);
		double threshold = .9d;

		return (dataSourceBySymbol, times) -> {
			Map<String, float[]> ema = dataSourceBySymbol //
					.mapValue(dataSource -> ma.exponentialMovingAvg(dataSource.prices, decay)) //
					.toMap();

			return (time, index) -> dataSourceBySymbol //
					.map2((symbol, dataSource) -> {
						int last = index - 1;
						float lastEma = ema.get(symbol)[last];
						float latest = dataSource.prices[last];
						return latest / lastEma < threshold ? 1d : 0d;
					}) //
					.toList();
		};
	}

	public static BackAllocator byLastPriceChange() {
		return (dataSourceBySymbol, times) -> (time, index) -> dataSourceBySymbol //
				.map2((symbol, dataSource) -> dataSource.get(index - 2).price / dataSource.get(index - 1).price < .96d ? 1d : 0d) //
				.toList();
	}

	public static BackAllocator byPairs(Configuration cfg, String symbol0, String symbol1) {
		return BackAllocator_ //
				.relativeToIndex(cfg, symbol0, rsi_(32, .3d, .7d)) //
				.filterAssets(symbol -> String_.equals(symbol, symbol1));
	}

	public static BackAllocator byReturnsProRata() {
		return (dataSourceBySymbol, times) -> (time, index) -> {
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

	public static BackAllocator byWorstReturn() {
		return (dataSourceBySymbol, times) -> (time, index) -> dataSourceBySymbol //
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

	public static BackAllocator donchian() {
		int window = 32;

		BackAllocator ba0 = (dataSourceBySymbol, times) -> {
			Map<String, Donchian> donchianBySymbol = dataSourceBySymbol //
					.mapValue(dataSource -> ts.donchian(window, dataSource.prices)) //
					.toMap();

			return (time, index) -> dataSourceBySymbol //
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
		};

		return ba0.unleverage();
	}

	public static BackAllocator dump(BackAllocator backAllocator0) {
		return (dataSourceBySymbol, times) -> {
			OnDateTime onDateTime = backAllocator0.allocate(dataSourceBySymbol, times);

			return (time, index) -> {
				List<Pair<String, Double>> ratioBySymbol = onDateTime.onDateTime(time, index);
				System.out.println("ratioBySymbol = " + ratioBySymbol);
				return ratioBySymbol;
			};
		};
	}

	public static BackAllocator even(BackAllocator backAllocator0) {
		BackAllocator backAllocator1 = backAllocator0.filterShorts();

		return (dataSourceBySymbol, times) -> {
			OnDateTime onDateTime = backAllocator1.allocate(dataSourceBySymbol, times);

			return (time, index) -> {
				List<Pair<String, Double>> potentialBySymbol = onDateTime.onDateTime(time, index);
				double each = 1d / Read.from2(potentialBySymbol).size();

				return Read.from2(potentialBySymbol) //
						.filterKey(symbol -> !String_.equals(symbol, Asset.cashSymbol)) //
						.mapValue(potential -> 1d / each) //
						.toList();
			};
		};
	}

	public static BackAllocator movingAvg() {
		int nPastDays = 64;
		int nHoldDays = 8;
		float threshold = .15f;
		Strategos strategos = new Strategos();
		BuySellStrategy mamr = strategos.movingAvgMeanReverting(nPastDays, nHoldDays, threshold);

		BackAllocator ba0 = (dataSourceBySymbol, times) -> {
			Map<String, GetBuySell> getBuySellBySymbol = dataSourceBySymbol //
					.mapValue(dataSource -> mamr.analyze(dataSource.prices)) //
					.toMap();

			return (time, index) -> dataSourceBySymbol //
					.map2((symbol, dataSource) -> {
						GetBuySell gbs = getBuySellBySymbol.get(symbol);
						int hold = 0;
						for (int i = 0; i < index; i++)
							hold += gbs.get(i);
						return (double) hold;
					}) //
					.toList();
		};

		return ba0.unleverage();
	}

	public static BackAllocator movingMedianMeanReversion() {
		int windowSize0 = 1;
		int windowSize1 = 32;

		BackAllocator ba0 = (dataSourceBySymbol, times) -> {
			Map<String, float[]> movingMedian0BySymbol = dataSourceBySymbol //
					.mapValue(dataSource -> ma.movingMedian(dataSource.prices, windowSize0)) //
					.toMap();

			Map<String, float[]> movingMedian1BySymbol = dataSourceBySymbol //
					.mapValue(dataSource -> ma.movingMedian(dataSource.prices, windowSize1)) //
					.toMap();

			return (time, index) -> dataSourceBySymbol //
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
		};

		return ba0.unleverage();
	}

	public static BackAllocator ofSingle(String symbol) {
		return (dataSourceBySymbol, times) -> (time, index) -> Arrays.asList(Pair.of(symbol, 1d));
	}

	public static BackAllocator questoQuella(String symbol0, String symbol1) {
		int tor = 64;
		double threshold = 0d;

		BackAllocator ba0 = (dataSourceBySymbol, times) -> {
			Map<String, DataSource> dataSources = dataSourceBySymbol.toMap();
			DataSource dataSource0 = dataSources.get(symbol0);
			DataSource dataSource1 = dataSources.get(symbol1);

			return (time, index) -> {
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
		};

		return ba0.filterAssets(symbol -> String_.equals(symbol, symbol0) || String_.equals(symbol, symbol1));
	}

	public static BackAllocator relative(BackAllocator backAllocator, DataSource index) {
		return relative_(backAllocator, index);
	}

	public static BackAllocator relativeToHsi(Configuration cfg, BackAllocator backAllocator) {
		return relativeToIndex(cfg, "^HSI", backAllocator);
	}

	public static BackAllocator relativeToIndex(Configuration cfg, String indexSymbol, BackAllocator backAllocator) {
		return relative_(backAllocator, cfg.dataSource(indexSymbol));
	}

	public static BackAllocator rsi() {
		return rsi_(32, .3d, .7d);
	}

	public static BackAllocator variableBollingerBands() {
		BackAllocator ba0 = (dataSourceBySymbol, times) -> {
			return (time, index) -> dataSourceBySymbol //
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
		};

		return ba0.unleverage();
	}

	public static BackAllocator threeMovingAvgs() {
		BackAllocator ba0 = (dataSourceBySymbol, times) -> {
			Map<String, float[]> movingAvg0BySymbol = dataSourceBySymbol //
					.mapValue(dataSource -> ma.exponentialMovingGeometricAvg(dataSource.prices, .11d)) //
					.toMap();

			Map<String, float[]> movingAvg1BySymbol = dataSourceBySymbol //
					.mapValue(dataSource -> ma.exponentialMovingGeometricAvg(dataSource.prices, .08d)) //
					.toMap();

			Map<String, float[]> movingAvg2BySymbol = dataSourceBySymbol //
					.mapValue(dataSource -> ma.exponentialMovingGeometricAvg(dataSource.prices, .05d)) //
					.toMap();

			return (time, index) -> dataSourceBySymbol //
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
		};

		return ba0.unleverage();
	}

	private static BackAllocator bollingerBands_(float k) {
		return bollingerBands_(32, 0, k);
	}

	private static BackAllocator bollingerBands_(int backPos0, int backPos1, float k) {
		BackAllocator ba0 = (dataSourceBySymbol, times) -> {
			Map<String, float[]> percentbBySymbol = dataSourceBySymbol //
					.mapValue(dataSource -> bb.bb(dataSource.prices, backPos0, backPos1, k).percentb) //
					.toMap();

			return (time, index) -> dataSourceBySymbol //
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
		};

		return ba0.unleverage();
	}

	private static BackAllocator relative_(BackAllocator backAllocator, DataSource indexDataSource) {
		return (dataSourceBySymbol0, times_) -> {
			Streamlet2<String, DataSource> dataSourceBySymbol1 = dataSourceBySymbol0 //
					.mapValue(dataSource0 -> {
						String[] times = dataSource0.dates;
						float[] prices = dataSource0.prices;
						String[] indexDates = indexDataSource.dates;
						float[] indexPrices = indexDataSource.prices;
						int length = times.length;
						int indexLength = indexDates.length;
						float[] prices1 = new float[length];
						int ii = 0;

						for (int di = 0; di < length; di++) {
							String date = times[di];
							while (ii < indexLength && indexDates[ii].compareTo(date) < 0)
								ii++;
							prices1[di] = prices[di] / indexPrices[ii];
						}

						return new DataSource(times, prices1);
					}) //
					.collect(As::streamlet2);

			return backAllocator.allocate(dataSourceBySymbol1, times_)::onDateTime;
		};
	}

	private static BackAllocator rsi_(int window, double threshold0, double threshold1) {
		BackAllocator ba0 = (dataSourceBySymbol, times) -> (time, index) -> dataSourceBySymbol //
				.mapValue(dataSource -> {
					float[] prices = dataSource.prices;
					int gt = 0, ge = 0;
					for (int i = index - window; i < index; i++) {
						int compare = Float.compare(prices[i - 1], prices[i]);
						gt += compare < 0 ? 1 : 0;
						ge += compare <= 0 ? 1 : 0;
					}
					double rsigt = (double) gt / window;
					double rsige = (double) ge / window;
					if (rsige < threshold0) // over-sold
						return .5d - rsige;
					else if (threshold1 < rsigt) // over-bought
						return .5d - rsigt;
					else
						return 0d;
				}) //
				.toList();

		return ba0.unleverage();
	}

}
