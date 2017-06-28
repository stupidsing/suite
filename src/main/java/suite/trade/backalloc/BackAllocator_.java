package suite.trade.backalloc;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import suite.adt.pair.Pair;
import suite.math.stat.BollingerBands;
import suite.math.stat.Statistic;
import suite.math.stat.Statistic.MeanVariance;
import suite.streamlet.As;
import suite.streamlet.Read;
import suite.streamlet.Streamlet;
import suite.streamlet.Streamlet2;
import suite.trade.MovingAverage;
import suite.trade.MovingAverage.MovingRange;
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

	public static BackAllocator bollingerBands() {
		return bollingerBands_(32, 0, 2f);
	}

	public static BackAllocator cash() {
		return (dsBySymbol, times) -> (time, index) -> Collections.emptyList();
	}

	public static BackAllocator donchian(int window) {
		return (dsBySymbol, times) -> {
			Map<String, MovingRange[]> movingRangeBySymbol = dsBySymbol //
					.mapValue(ds -> ma.movingRange(ds.prices, window)) //
					.toMap();

			return (time, index) -> dsBySymbol //
					.map2((symbol, ds) -> {
						MovingRange[] movingRange = movingRangeBySymbol.get(symbol);
						float price = ds.prices[index - 1];
						double hold = 0d;
						for (int i = 0; i < index; i++) {
							float min = movingRange[i].min;
							float max = movingRange[i].max;
							float median = movingRange[i].median;
							if (price <= min)
								hold = 1d;
							else if (price < median)
								hold = Math.max(0d, hold);
							else if (price < max)
								hold = Math.min(0d, hold);
							else
								hold = -1d;
						}
						return hold;
					}) //
					.toList();
		};
	}

	public static BackAllocator ema() {
		int halfLife = 2;
		double scale = 1d / Math.log(.8d);

		return (dsBySymbol, times) -> {
			Map<String, float[]> ema = dsBySymbol //
					.mapValue(ds -> ma.exponentialMovingAvg(ds.prices, halfLife)) //
					.toMap();

			return (time, index) -> dsBySymbol //
					.map2((symbol, ds) -> {
						int last = index - 1;
						double lastEma = ema.get(symbol)[last];
						double latest = ds.prices[last];
						return Math.log(latest / lastEma) * scale;
					}) //
					.toList();
		};
	}

	public static BackAllocator lastReturn(int nWorsts, int nBests) {
		return (dsBySymbol, times) -> (time, index) -> {
			List<String> list = dsBySymbol //
					.map2((symbol, ds) -> {
						float[] prices = ds.prices;
						double price0 = prices[index - 2];
						double price1 = prices[index - 1];
						return price1 / price0 - 1d;
					}) //
					.sortBy((symbol, return_) -> return_) //
					.keys() //
					.toList();

			int size = list.size();

			return Streamlet //
					.concat(Read.from(list.subList(0, nWorsts)), Read.from(list.subList(size - nBests, size))) //
					.map2(symbol -> 1d / (nWorsts + nBests)) //
					.toList();
		};
	}

	public static BackAllocator lastReturnsProRata() {
		return (dsBySymbol, times) -> (time, index) -> {
			Streamlet2<String, Double> returns = dsBySymbol //
					.map2((symbol, ds) -> {
						float[] prices = ds.prices;
						double price0 = prices[index - 2];
						double price1 = prices[index - 1];
						return (price0 - price1) / price0;
					}) //
					.filterValue(return_ -> 0d < return_) //
					.collect(As::streamlet2);

			double sum = returns.collectAsDouble(As.sumOfDoubles((symbol, price) -> price));
			return returns.mapValue(return_ -> return_ / sum).toList();
		};
	}

	public static BackAllocator movingAvg() {
		int nPastDays = 64;
		int nHoldDays = 8;
		float threshold = .15f;
		Strategos strategos = new Strategos();
		BuySellStrategy mamr = strategos.movingAvgMeanReverting(nPastDays, nHoldDays, threshold);

		return (dsBySymbol, times) -> {
			Map<String, GetBuySell> getBuySellBySymbol = dsBySymbol //
					.mapValue(ds -> mamr.analyze(ds.prices)) //
					.toMap();

			return (time, index) -> dsBySymbol //
					.map2((symbol, ds) -> {
						GetBuySell gbs = getBuySellBySymbol.get(symbol);
						int hold = 0;
						for (int i = 0; i < index; i++)
							hold += gbs.get(i);
						return (double) hold;
					}) //
					.toList();
		};
	}

	public static BackAllocator movingAvgMedian() {
		int windowSize0 = 4;
		int windowSize1 = 12;

		return (dsBySymbol, times) -> {
			Map<String, float[]> movingAvg0BySymbol = dsBySymbol //
					.mapValue(ds -> ma.movingAvg(ds.prices, windowSize0)) //
					.toMap();

			Map<String, float[]> movingAvg1BySymbol = dsBySymbol //
					.mapValue(ds -> ma.movingAvg(ds.prices, windowSize1)) //
					.toMap();

			Map<String, MovingRange[]> movingRange0BySymbol = dsBySymbol //
					.mapValue(ds -> ma.movingRange(ds.prices, windowSize0)) //
					.toMap();

			Map<String, MovingRange[]> movingRange1BySymbol = dsBySymbol //
					.mapValue(ds -> ma.movingRange(ds.prices, windowSize1)) //
					.toMap();

			return (time, index) -> dsBySymbol //
					.map2((symbol, ds) -> {
						int last = index - 1;
						float movingAvg0 = movingAvg0BySymbol.get(symbol)[last];
						float movingAvg1 = movingAvg1BySymbol.get(symbol)[last];
						float movingMedian0 = movingRange0BySymbol.get(symbol)[last].median;
						float movingMedian1 = movingRange1BySymbol.get(symbol)[last].median;
						if (movingAvg0 < movingMedian0 && movingAvg1 < movingMedian1)
							return 1d;
						else if (movingMedian0 < movingAvg0 && movingMedian1 < movingAvg1)
							return -1d;
						else
							return 0d;
					}) //
					.toList();
		};
	}

	public static BackAllocator movingMedianMeanRevn() {
		int windowSize0 = 1;
		int windowSize1 = 32;

		return (dsBySymbol, times) -> {
			Map<String, MovingRange[]> movingMedian0BySymbol = dsBySymbol //
					.mapValue(ds -> ma.movingRange(ds.prices, windowSize0)) //
					.toMap();

			Map<String, MovingRange[]> movingMedian1BySymbol = dsBySymbol //
					.mapValue(ds -> ma.movingRange(ds.prices, windowSize1)) //
					.toMap();

			return (time, index) -> dsBySymbol //
					.map2((symbol, ds) -> {
						MovingRange[] movingRange0 = movingMedian0BySymbol.get(symbol);
						MovingRange[] movingRange1 = movingMedian1BySymbol.get(symbol);
						int last = index - 1;
						double median0 = movingRange0[last].median;
						double median1 = movingRange1[last].median;
						double ratio = median1 / median0;
						return ratio - 1d;
					}) //
					.toList();
		};
	}

	public static BackAllocator ofSingle(String symbol) {
		return (dsBySymbol, times) -> (time, index) -> Arrays.asList(Pair.of(symbol, 1d));
	}

	public static BackAllocator pairs(Configuration cfg, String symbol0, String symbol1) {
		return BackAllocator_ //
				.rsi_(32, .3d, .7d) //
				.relativeToIndex(cfg, symbol0) //
				.filterAssets(symbol -> String_.equals(symbol, symbol1));
	}

	public static BackAllocator questoQuella(String symbol0, String symbol1) {
		int tor = 64;
		double threshold = 0d;

		BackAllocator ba0 = (dsBySymbol, times) -> {
			Map<String, DataSource> dsBySymbol_ = dsBySymbol.toMap();
			DataSource ds0 = dsBySymbol_.get(symbol0);
			DataSource ds1 = dsBySymbol_.get(symbol1);

			return (time, index) -> {
				int ix = index - 1;
				int i0 = ix - tor;
				double p0 = ds0.get(i0).price, px = ds0.get(ix).price;
				double q0 = ds1.get(i0).price, qx = ds1.get(ix).price;
				double pdiff = (px - p0) / px;
				double qdiff = (qx - q0) / qx;

				if (threshold < Math.abs(pdiff - qdiff))
					return Arrays.asList( //
							Pair.of(pdiff < qdiff ? symbol0 : symbol1, 1d), //
							Pair.of(pdiff < qdiff ? symbol1 : symbol0, -1d));
				else
					return Collections.emptyList();
			};
		};

		return ba0.filterAssets(symbol -> String_.equals(symbol, symbol0) || String_.equals(symbol, symbol1));
	}

	public static BackAllocator rsi() {
		return rsi_(32, .3d, .7d);
	}

	public static BackAllocator variableBollingerBands() {
		return (dsBySymbol, times) -> {
			return (time, index) -> dsBySymbol //
					.map2((symbol, ds) -> {
						int last = index - 1;
						double hold = 0d;

						for (int window = 1; hold == 0d && window < 256; window++) {
							float price = ds.prices[last];
							MeanVariance mv = stat.meanVariance(Arrays.copyOfRange(ds.prices, last - window, last));
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
	}

	public static BackAllocator tripleMovingAvgs() {
		return (dsBySymbol, times) -> {
			Map<String, float[]> movingAvg0BySymbol = dsBySymbol //
					.mapValue(ds -> ma.exponentialMovingGeometricAvg(ds.prices, 18)) //
					.toMap();

			Map<String, float[]> movingAvg1BySymbol = dsBySymbol //
					.mapValue(ds -> ma.exponentialMovingGeometricAvg(ds.prices, 6)) //
					.toMap();

			Map<String, float[]> movingAvg2BySymbol = dsBySymbol //
					.mapValue(ds -> ma.exponentialMovingGeometricAvg(ds.prices, 2)) //
					.toMap();

			return (time, index) -> dsBySymbol //
					.map2((symbol, ds) -> {
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
	}

	private static BackAllocator bollingerBands_(int backPos0, int backPos1, float k) {
		return (dsBySymbol, times) -> {
			Map<String, float[]> percentbBySymbol = dsBySymbol //
					.mapValue(ds -> bb.bb(ds.prices, backPos0, backPos1, k).percentb) //
					.toMap();

			return (time, index) -> dsBySymbol //
					.map2((symbol, ds) -> {
						float[] percentbs = percentbBySymbol.get(symbol);
						double hold = 0d;
						for (int i = 0; i < index; i++) {
							float percentb = percentbs[i];
							if (percentb <= 0f)
								hold = 1d;
							else if (.5f < percentb) // un-short
								hold = Math.max(0d, hold);
							else if (percentb < 1f) // un-long
								hold = Math.min(0d, hold);
							else
								hold = -1d;
						}
						return hold;
					}) //
					.toList();
		};
	}

	private static BackAllocator rsi_(int window, double threshold0, double threshold1) {
		return (dsBySymbol, times) -> (time, index) -> dsBySymbol //
				.mapValue(ds -> {
					float[] prices = ds.prices;
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
	}

}
