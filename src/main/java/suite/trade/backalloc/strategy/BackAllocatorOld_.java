package suite.trade.backalloc.strategy;

import java.util.Arrays;
import java.util.Collections;
import java.util.Map;

import suite.adt.pair.Pair;
import suite.math.stat.BollingerBands;
import suite.math.stat.Quant;
import suite.math.stat.Statistic;
import suite.math.stat.Statistic.MeanVariance;
import suite.math.stat.TimeSeries;
import suite.streamlet.Streamlet2;
import suite.trade.MovingAverage;
import suite.trade.MovingAverage.MovingRange;
import suite.trade.backalloc.BackAllocator;
import suite.trade.data.Configuration;
import suite.trade.data.DataSource;
import suite.util.String_;

public class BackAllocatorOld_ {

	public static BackAllocatorOld_ me = new BackAllocatorOld_();

	private BollingerBands bb = new BollingerBands();
	private MovingAverage ma = new MovingAverage();
	private Statistic stat = new Statistic();
	private TimeSeries ts = new TimeSeries();

	private BackAllocatorOld_() {
	}

	public BackAllocator bbSlope() {
		return BackAllocator.byPrices(prices -> {
			float[] percentbs = bb.bb(prices, 32, 0, 2f).percentbs;
			float[] ma_ = ma.movingAvg(percentbs, 6);
			float[] diffs = ts.differences(3, ma_);

			return index -> {
				int last = index - 1;
				float percentb = ma_[last];
				float diff = diffs[last];
				if (percentb < .2d && .015d < diff)
					return 1d;
				else if (-.8d < percentb && diff < -.015d)
					return -1d;
				else
					return 0d;
			};
		});
	}

	public BackAllocator bbVariable() {
		return BackAllocator.byPrices(prices -> Quant.filterRange(1, index -> {
			int last = index - 1;
			double hold = 0d;

			for (int window = 1; hold == 0d && window < 256; window++) {
				float price = prices[last];
				MeanVariance mv = stat.meanVariance(Arrays.copyOfRange(prices, last - window, last));
				double mean = mv.mean;
				double diff = 3d * mv.standardDeviation();

				if (price < mean - diff)
					hold = 1d;
				else if (mean + diff < price)
					hold = -1d;
			}

			return hold;
		}));
	}

	public BackAllocator movingAvgMedian() {
		int windowSize0 = 4;
		int windowSize1 = 12;

		return BackAllocator.byPrices(prices -> {
			float[] movingAvgs0 = ma.movingAvg(prices, windowSize0);
			float[] movingAvgs1 = ma.movingAvg(prices, windowSize1);
			MovingRange[] movingRanges0 = ma.movingRange(prices, windowSize0);
			MovingRange[] movingRanges1 = ma.movingRange(prices, windowSize1);

			return index -> {
				int last = index - 1;
				float movingAvg0 = movingAvgs0[last];
				float movingAvg1 = movingAvgs1[last];
				float movingMedian0 = movingRanges0[last].median;
				float movingMedian1 = movingRanges1[last].median;
				int sign0 = Quant.sign(movingAvg0, movingMedian0);
				int sign1 = Quant.sign(movingAvg1, movingMedian1);
				return sign0 == sign1 ? (double) sign0 : 0d;
			};
		});
	}

	public BackAllocator movingMedianMeanRevn() {
		int windowSize0 = 1;
		int windowSize1 = 32;

		return BackAllocator.byPrices(prices -> {
			MovingRange[] movingRanges0 = ma.movingRange(prices, windowSize0);
			MovingRange[] movingRanges1 = ma.movingRange(prices, windowSize1);

			return index -> {
				int last = index - 1;
				return Quant.return_(movingRanges0[last].median, movingRanges1[last].median);
			};
		});
	}

	public BackAllocator pairs(Configuration cfg, String symbol0, String symbol1) {
		return BackAllocator_.me.rsi //
				.relativeToIndex(cfg, symbol0) //
				.filterByAsset(symbol -> String_.equals(symbol, symbol1));
	}

	public BackAllocator questoQuella(String symbol0, String symbol1) {
		int tor = 64;
		double threshold = 0d;

		BackAllocator ba0 = (akds, indices) -> {
			Streamlet2<String, DataSource> dsBySymbol = akds.dsByKey;
			Map<String, DataSource> dsBySymbol_ = dsBySymbol.toMap();
			DataSource ds0 = dsBySymbol_.get(symbol0);
			DataSource ds1 = dsBySymbol_.get(symbol1);

			return index -> {
				int ix = index - 1;
				int i0 = ix - tor;
				double p0 = ds0.get(i0).t1, px = ds0.get(ix).t1;
				double q0 = ds1.get(i0).t1, qx = ds1.get(ix).t1;
				double pdiff = Quant.return_(p0, px);
				double qdiff = Quant.return_(q0, qx);

				if (threshold < Math.abs(pdiff - qdiff))
					return Arrays.asList( //
							Pair.of(pdiff < qdiff ? symbol0 : symbol1, 1d), //
							Pair.of(pdiff < qdiff ? symbol1 : symbol0, -1d));
				else
					return Collections.emptyList();
			};
		};

		return ba0.filterByAsset(symbol -> String_.equals(symbol, symbol0) || String_.equals(symbol, symbol1));
	}

	// reverse draw-down; trendy strategy
	public BackAllocator revDrawdown() {
		return BackAllocator.byPrices(prices -> index -> {
			int i = index - 1;
			int i0 = Math.max(0, i - 128);
			int ix = i;
			int dir = 0;

			float lastPrice = prices[ix];
			float priceo = lastPrice;
			int io = i;

			for (; i0 <= i; i--) {
				float price = prices[i];
				int dir1 = Quant.sign(price, lastPrice);

				if (dir != 0 && dir != dir1) {
					double r = (index - io) / (double) (index - i);
					return .36d < r ? Quant.return_(priceo, lastPrice) * r * 4d : 0d;
				} else
					dir = dir1;

				if (Quant.sign(price, priceo) == dir) {
					priceo = price;
					io = i;
				}
			}

			return 0d;
		});
	}

}
