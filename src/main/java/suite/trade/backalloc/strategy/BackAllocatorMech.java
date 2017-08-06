package suite.trade.backalloc.strategy;

import suite.adt.pair.Fixie;
import suite.math.stat.BollingerBands;
import suite.math.stat.Quant;
import suite.primitive.Floats_;
import suite.trade.MovingAverage;
import suite.trade.MovingAverage.Macd;
import suite.trade.MovingAverage.MovingRange;
import suite.trade.Oscillator;
import suite.trade.backalloc.BackAllocator;

/**
 * "Mechanical Trading Systems."
 *
 * @author ywsing
 */
public class BackAllocatorMech {

	private static BollingerBands bb = new BollingerBands();
	private static MovingAverage ma = new MovingAverage();
	private static Oscillator osc = new Oscillator();

	public static BackAllocator bollingerBands() {
		return BackAllocator.byPrices(prices -> {
			float[] percentbs = bb.bb(prices, 20, 0, 2f).percentbs;
			return BackAllocator_.fold(0, percentbs.length, (i, hold) -> -Quant.hold(hold, percentbs[i], -1f, 0f, 1f));
		});
	}

	public static BackAllocator channelBreakout() {
		return BackAllocator.byPrices(prices -> {
			MovingRange[] movingRanges = ma.movingRange(prices, 20);

			return BackAllocator_.fold(0, prices.length, (i, hold) -> {
				MovingRange movingRange = movingRanges[i];
				return -Quant.hold(hold, prices[i], movingRange.min, movingRange.max);
			});
		});
	}

	public static BackAllocator dmi() {
		return BackAllocator.byDataSource(ds -> {
			float[] dmis = osc.dmi(ds);
			return BackAllocator_.fold(0, dmis.length, (i, hold) -> -Quant.hold(hold, dmis[i], -.2d, 0d, .2d));
		});
	}

	public static BackAllocator dmiAdx() {
		return BackAllocator.byDataSource(ds -> {
			int length = ds.ts.length;
			float[] dmis = osc.dmi(ds);
			float[] adxs = ma.movingAvg(Floats_.toArray(length, Math::abs), 9);

			return BackAllocator_.fold(1, length, (i, hold) -> .2d <= adxs[i] ? -Quant.hold(hold, dmis[i], -.2d, 0d, .2d) : 0f);
		});
	}

	public static BackAllocator macd() {
		return BackAllocator.byPrices(prices -> {
			Macd macd = ma.macd(prices);
			return index -> {
				int last = index - 1;
				float macd_ = macd.macds[last];
				float movingAvgMacd = macd.movingAvgMacds[last];
				int sign0 = Quant.sign(movingAvgMacd, macd_);
				return sign0 == Quant.sign(0d, movingAvgMacd) ? (double) sign0 : 0d;
			};
		});
	}

	// two moving average cross-over
	public static BackAllocator ma2() {
		return BackAllocator.byPrices(prices -> {
			float[] movingAvgs0 = ma.movingAvg(prices, 26);
			float[] movingAvgs1 = ma.movingAvg(prices, 9);
			return Quant.filterRange(1, index -> {
				int last = index - 1;
				return (double) Quant.sign(movingAvgs0[last], movingAvgs1[last]);
			});
		});
	}

	// Ichimoku two moving average cross-over
	public static BackAllocator ma2Ichimoku() {
		return BackAllocator.byPrices(prices -> {
			float[] movingAvgs0 = ma.movingAvg(prices, 26);
			float[] movingAvgs1 = ma.movingAvg(prices, 9);
			return Quant.filterRange(2, index -> {
				int last = index - 1;
				float movingAvg0 = movingAvgs0[last];
				float movingAvg1 = movingAvgs1[last];
				float movingAvg0ytd = movingAvgs0[last - 1]; // all my troubles seem so far away
				int sign = Quant.sign(movingAvg0, movingAvg1);
				return sign == Quant.sign(movingAvg0ytd, movingAvg0) ? (double) sign : 0d;
			});
		});
	}

	// three moving average cross-over
	public static BackAllocator ma3() {
		return BackAllocator_.tripleMovingAvgs(prices -> Fixie.of( //
				ma.movingAvg(prices, 52), //
				ma.movingAvg(prices, 26), //
				ma.movingAvg(prices, 9)));

	}

	// Ichimoku three moving average cross-over
	public static BackAllocator ma3ichimoku() {
		return BackAllocator.byPrices(prices -> {
			int length = prices.length;
			float[] movingAvgs0 = ma.movingAvg(prices, 52);
			float[] movingAvgs1 = ma.movingAvg(prices, 26);
			float[] movingAvgs2 = ma.movingAvg(prices, 9);

			return BackAllocator_.fold(1, length, (i, hold) -> {
				int im1 = i - 1;
				float movingAvg0 = movingAvgs0[i];
				float movingAvg1 = movingAvgs1[i];
				float movingAvg2 = movingAvgs2[i];
				float movingAvg1ytd = movingAvgs1[im1]; // all my troubles seem so far away
				float movingAvg2ytd = movingAvgs2[im1];
				int sign0 = Quant.sign(movingAvg0, movingAvg1);
				int sign1 = Quant.sign(movingAvg1, movingAvg2);
				int sign2 = Quant.sign(movingAvg1, movingAvg1ytd);
				int sign3 = Quant.sign(movingAvg2, movingAvg2ytd);
				boolean b1 = sign0 == sign1 && sign1 == sign2 && (hold != 0f || sign2 == sign3);
				return b1 ? (float) -sign0 : 0f;
			});
		});
	}

}
