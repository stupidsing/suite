package suite.trade.backalloc.strategy;

import java.util.function.IntPredicate;

import suite.adt.pair.Fixie;
import suite.math.stat.BollingerBands;
import suite.math.stat.BollingerBands.Bb;
import suite.math.stat.Quant;
import suite.primitive.IntInt_Int;
import suite.trade.MovingAverage;
import suite.trade.MovingAverage.Macd;
import suite.trade.MovingAverage.MovingRange;
import suite.trade.Oscillator;
import suite.trade.Oscillator.Dmi;
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
			float[] dmis = osc.dmi(ds).dmi;
			return BackAllocator_.fold(0, dmis.length, (i, hold) -> -Quant.hold(hold, dmis[i], -.2d, 0d, .2d));
		});
	}

	public static BackAllocator dmiAdx() {
		return BackAllocator.byDataSource(ds -> {
			int length = ds.ts.length;
			Dmi dmi = osc.dmi(ds);
			float[] dmis = dmi.dmi;
			float[] adxs = dmi.adx(9);

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

	public static BackAllocator mrBbAdx() {
		return BackAllocator //
				.byDataSource(ds -> {
					float[] prices = ds.prices;
					Bb bb_ = bb.bb(prices, 20, 0, 2f);
					float[] adxs = osc.dmi(ds).adx(9);

					return BackAllocator_.fold(1, prices.length, (i, hold) -> {
						if (hold == 0f && adxs[i] < .2f)
							if (cross(i, bb_.uppers, prices))
								return -1f;
							else if (cross(i, prices, bb_.lowers))
								return 1f;
							else
								return hold;
						else
							return hold;
					});
				}) //
				.stop(.9875d, .9875d);
	}

	public static BackAllocator mrBbMa200() {
		return BackAllocator //
				.byPrices(prices -> {
					float[] movingAvgs = ma.movingAvg(prices, 200);
					Bb bb_ = bb.bb(prices, 20, 0, 2f);
					float[] lowers = bb_.lowers;
					float[] uppers = bb_.uppers;

					return BackAllocator_.fold(1, prices.length, (i, hold) -> {
						float movingAvg = movingAvgs[i];
						float price = prices[i];
						if (hold < 0f)
							return price < uppers[i] ? hold : 0f;
						else if (0f < hold)
							return lowers[i] < price ? hold : 0f;
						else if (cross(i, uppers, prices) && price < movingAvg)
							return -1f;
						else if (cross(i, prices, lowers) && movingAvg < price)
							return 1f;
						else
							return hold;
					});
				}) //
				.stopLoss(.975d);
	}

	public static BackAllocator mrRsiMa200() {
		return BackAllocator //
				.byPrices(prices -> {
					float[] movingAvgs = ma.movingAvg(prices, 200);
					float[] rsi0 = osc.rsi(prices, 14);
					float[] rsi1 = osc.rsi(prices, 9);

					return BackAllocator_.fold(1, prices.length, (i, hold) -> {
						float movingAvg = movingAvgs[i];
						float price = prices[i];
						float rsi1_ = rsi1[i];
						if (hold < 0f)
							return !cross(i, i_ -> rsi0[i_] < .4f) ? hold : 0f;
						else if (0f < hold)
							return !cross(i, i_ -> .6f < rsi0[i_]) ? hold : 0f;
						else if (price < movingAvg && .65f < rsi1_) // over-bought
							return -1f;
						else if (movingAvg < price && rsi1_ < .35f) // over-sold
							return 1f;
						else
							return hold;
					});
				}) //
				.stopLoss(.975d);
	}

	// slow stochastics extremes with commodity channel index
	public static BackAllocator mrSseCci() {
		return mrSseCciTimedExit(Integer.MAX_VALUE);
	}

	public static BackAllocator mrSseCciTimedExit() {
		return mrSseCciTimedExit(14);
	}

	// eight-days open close
	public static BackAllocator openClose8() {
		return BackAllocator //
				.byDataSource(ds -> {
					float[] movingAvgOps = ma.movingAvg(ds.opens, 8);
					float[] movingAvgCls = ma.movingAvg(ds.closes, 8);
					return index -> Quant.sign(movingAvgOps[index], movingAvgCls[index]) * 1d;
				});
	}

	// seven-period reversal
	public static BackAllocator period7(int timedExit) {
		return BackAllocator //
				.byPrices(prices -> {
					IntInt_Int signs = (s, e) -> {
						int n = 0;
						for (int i = s; i < e; i++)
							n += Quant.sign(prices[i - 1], prices[i]);
						return n;
					};

					return BackAllocator_.fold(7, prices.length, (i, hold) -> {
						if (hold < 0f)
							return -6 < signs.apply(i - 5, i + 1) ? hold : 0f;
						else if (0f < hold)
							return signs.apply(i - 5, i + 1) < 6 ? hold : 0f;
						else {
							int n = Quant.sign(prices[i - 1], prices[i]) - signs.apply(i - 6, i);
							if (n == -7)
								return -1f;
							else if (n == 7)
								return 1f;
							else
								return hold;
						}
					});
				}) //
				.stop(.99f, 1.01f);
	}

	public static BackAllocator rsiCrossover() {
		return BackAllocator //
				.byPrices(prices -> {
					float[] rsi = osc.rsi(prices, 14);

					return BackAllocator_.fold(1, prices.length, (i, hold) -> {
						if (hold == 0f)
							if (.75f < rsi[i - 1] && cross(i, i_ -> rsi[i_] < .75f))
								return 1f;
							else if (rsi[i - 1] < .25f && cross(i, i_ -> .25f < rsi[i_]))
								return 1f;
							else
								return hold;
						else
							return hold;
					});
				}) //
				.stop(.99f, 1.03f);
	}

	private static BackAllocator mrSseCciTimedExit(int timedExit) {
		return BackAllocator //
				.byDataSource(ds -> {
					float[] prices = ds.prices;
					float[] stos = osc.stochastic(ds, 14);
					float[] stoSlows = ma.movingAvg(stos, 3);
					float[] ccis = osc.cci(ds, 10);

					return BackAllocator_.fold(1, prices.length, timedExit, (i, hold) -> {
						if (hold < 0f)
							return !cross(i, i_ -> stoSlows[i_] < .7f) ? hold : 0f;
						else if (0f < hold)
							return !cross(i, i_ -> .3f < stoSlows[i_]) ? hold : 0f;
						else if (cross(i, i_ -> .85f < stoSlows[i_]) && 1f < ccis[i])
							return -1f;
						else if (cross(i, i_ -> stoSlows[i_] < .15f) && ccis[i] < -1f)
							return 1f;
						else
							return hold;
					});
				}) //
				.stopLoss(.985d);
	}

	private static boolean cross(int i, float[] fs0, float[] fs1) {
		return cross(i, i_ -> fs0[i_] < fs1[i_]);
	}

	private static boolean cross(int i, IntPredicate pred) {
		return !pred.test(i - 1) && pred.test(i);
	}

}
