package suite.trade.backalloc.strategy;

import java.util.function.IntPredicate;

import suite.adt.pair.Fixie;
import suite.math.stat.BollingerBands;
import suite.math.stat.BollingerBands.Bb;
import suite.math.stat.Quant;
import suite.primitive.IntInt_Int;
import suite.streamlet.Read;
import suite.streamlet.Streamlet2;
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

	public static BackAllocatorMech me = new BackAllocatorMech();

	public final Streamlet2<String, BackAllocator> baByName = Read //
			.<String, BackAllocator> empty2() //
			.cons("bb", bollingerBands()) //
			.cons("chanbrk", channelBreakout()) //
			.cons("dmi", dmi()) //
			.cons("dmiadx", dmiAdx()) //
			.cons("ma2", ma2()) //
			.cons("ma2i", ma2Ichimoku()) //
			.cons("ma3", ma3()) //
			.cons("ma3i", ma3ichimoku()) //
			.cons("macd", macd()) //
			.cons("bbadx", mrBbAdx()) //
			.cons("bbma200", mrBbMa200()) //
			.cons("rsima200", mrRsiMa200()) //
			.cons("ssecci", mrSseCci()) //
			.cons("sseccitx", mrSseCciTimedExit()) //
			.cons("p7rev", period7reversal()) //
			.cons("rsix", rsiCrossover());

	private BollingerBands bb = new BollingerBands();
	private MovingAverage ma = new MovingAverage();
	private Oscillator osc = new Oscillator();

	private BackAllocatorMech() {
	}

	private BackAllocator bollingerBands() {
		return BackAllocator_.byPrices(prices -> {
			float[] sds = bb.bb(prices, 20, 0, 2f).sds;
			return Quant.fold(0, sds.length, (i, hold) -> -Quant.hold(hold, sds[i], -.5d, 0d, .5d));
		});
	}

	private BackAllocator channelBreakout() {
		return BackAllocator_.byPrices(prices -> {
			MovingRange[] movingRanges = ma.movingRange(prices, 20);

			return Quant.fold(0, prices.length, (i, hold) -> {
				MovingRange movingRange = movingRanges[i];
				return -Quant.hold(hold, prices[i], movingRange.min, movingRange.max);
			});
		});
	}

	private BackAllocator dmi() {
		return BackAllocator_.byDataSource(ds -> {
			float[] dmis = osc.dmi(ds, 10).dmi;
			return Quant.fold(0, dmis.length, (i, hold) -> -Quant.hold(hold, dmis[i], -.2d, 0d, .2d));
		});
	}

	private BackAllocator dmiAdx() {
		return BackAllocator_.byDataSource(ds -> {
			Dmi dmi = osc.dmi(ds, 10);
			float[] dmis = dmi.dmi;
			float[] adxs = dmi.adx(9);
			return Quant.fold(0, dmis.length, (i, hold) -> .2d <= adxs[i] ? -Quant.hold(hold, dmis[i], -.2d, 0d, .2d) : 0f);
		});
	}

	private BackAllocator macd() {
		return BackAllocator_.byPrices(prices -> {
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
	private BackAllocator ma2() {
		return BackAllocator_.byPrices(prices -> {
			float[] movingAvgs0 = ma.movingAvg(prices, 26);
			float[] movingAvgs1 = ma.movingAvg(prices, 9);
			return Quant.filterRange(1, index -> {
				int last = index - 1;
				return (double) Quant.sign(movingAvgs0[last], movingAvgs1[last]);
			});
		});
	}

	// Ichimoku two moving average cross-over
	private BackAllocator ma2Ichimoku() {
		return BackAllocator_.byPrices(prices -> {
			float[] movingAvgs0 = ma.movingAvg(prices, 26);
			float[] movingAvgs1 = ma.movingAvg(prices, 9);
			return Quant.filterRange(2, index -> {
				int last = index - 1;
				float movingAvg0 = movingAvgs0[last];
				float movingAvg1 = movingAvgs1[last];
				float movingAvg0ytd = movingAvgs0[last - 1];
				int sign = Quant.sign(movingAvg0, movingAvg1);
				return sign == Quant.sign(movingAvg0ytd, movingAvg0) ? (double) sign : 0d;
			});
		});
	}

	// three moving average cross-over
	private BackAllocator ma3() {
		return BackAllocatorGeneral.me.tripleMovingAvgs(prices -> Fixie.of( //
				ma.movingAvg(prices, 52), //
				ma.movingAvg(prices, 26), //
				ma.movingAvg(prices, 9)));

	}

	// Ichimoku three moving average cross-over
	private BackAllocator ma3ichimoku() {
		return BackAllocator_.byPrices(prices -> {
			int length = prices.length;
			float[] movingAvgs0 = ma.movingAvg(prices, 52);
			float[] movingAvgs1 = ma.movingAvg(prices, 26);
			float[] movingAvgs2 = ma.movingAvg(prices, 9);

			return Quant.fold(1, length, (i, hold) -> {
				int im1 = i - 1;
				float movingAvg0 = movingAvgs0[i];
				float movingAvg1 = movingAvgs1[i];
				float movingAvg2 = movingAvgs2[i];
				float movingAvg1ytd = movingAvgs1[im1];
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

	private BackAllocator mrBbAdx() {
		return BackAllocator_ //
				.byDataSource(ds -> {
					float[] prices = ds.prices;
					Bb bb_ = bb.bb(prices, 20, 0, 2f);
					float[] adxs = osc.dmi(ds).adx(9);

					return Quant.enterKeep(1, prices.length, //
							i -> adxs[i] < .2f && cross(i, bb_.uppers, prices), //
							i -> adxs[i] < .2f && cross(i, prices, bb_.lowers), //
							i -> true, //
							i -> true);
				}) //
				.stop(.9875d, .9875d);
	}

	private BackAllocator mrBbMa200() {
		return BackAllocator_ //
				.byPrices(prices -> {
					float[] movingAvgs = ma.movingAvg(prices, 200);
					Bb bb_ = bb.bb(prices, 20, 0, 2f);
					float[] lowers = bb_.lowers;
					float[] uppers = bb_.uppers;
					float[] sds = bb_.sds;

					return Quant.enterKeep(1, prices.length, //
							i -> cross(i, uppers, prices) && prices[i] < movingAvgs[i], //
							i -> cross(i, prices, lowers) && movingAvgs[i] < prices[i], //
							i -> 0f <= sds[i], //
							i -> sds[i] <= 0f);
				}) //
				.stopLoss(.975d);
	}

	private BackAllocator mrRsiMa200() {
		return BackAllocator_ //
				.byPrices(prices -> {
					float[] movingAvgs = ma.movingAvg(prices, 200);
					float[] rsi0 = osc.rsi(prices, 14);
					float[] rsi1 = osc.rsi(prices, 9);

					return Quant.enterExit(1, prices.length, //
							i -> prices[i] < movingAvgs[i] && .65f < rsi1[i], //
							i -> movingAvgs[i] < prices[i] && rsi1[i] < .35f, //
							i -> cross(i, i_ -> rsi0[i_] < .4f), //
							i -> cross(i, i_ -> .6f < rsi0[i_]));
				}) //
				.stopLoss(.975d);
	}

	// slow stochastics extremes with commodity channel index
	private BackAllocator mrSseCci() {
		return mrSseCciTimedExit(Integer.MAX_VALUE);
	}

	private BackAllocator mrSseCciTimedExit() {
		return mrSseCciTimedExit(14);
	}

	// seven-period reversal
	private BackAllocator period7reversal() {
		return BackAllocator_ //
				.byPrices(prices -> {
					IntInt_Int signs = (s, e) -> {
						int n = 0;
						for (int i = s; i < e; i++)
							n += Quant.sign(prices[i - 1], prices[i]);
						return n;
					};

					return Quant.fold(7, prices.length, (i, hold) -> {
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

	private BackAllocator rsiCrossover() {
		return BackAllocator_ //
				.byPrices(prices -> {
					float[] rsi = osc.rsi(prices, 14);

					return Quant.enterKeep(1, prices.length, //
							i -> .75f < rsi[i - 1] && cross(i, i_ -> rsi[i_] < .75f), //
							i -> rsi[i - 1] < .25f && cross(i, i_ -> .25f < rsi[i_]), //
							i -> true, //
							i -> true);
				}) //
				.stop(.99f, 1.03f);
	}

	private BackAllocator mrSseCciTimedExit(int timedExit) {
		return BackAllocator_ //
				.byDataSource(ds -> {
					float[] prices = ds.prices;
					float[] stos = osc.stochastic(ds, 14);
					float[] stoSlows = ma.movingAvg(stos, 3);
					float[] ccis = osc.cci(ds, 10);

					return Quant.enterExit(1, prices.length, timedExit, //
							i -> cross(i, i_ -> .85f < stoSlows[i_]) && 1f < ccis[i], //
							i -> cross(i, i_ -> stoSlows[i_] < .15f) && ccis[i] < -1f, //
							i -> cross(i, i_ -> stoSlows[i_] < .7f), //
							i -> cross(i, i_ -> .3f < stoSlows[i_]));
				}) //
				.stopLoss(.985d);
	}

	private boolean cross(int i, float[] fs0, float[] fs1) {
		return cross(i, i_ -> fs0[i_] < fs1[i_]);
	}

	private boolean cross(int i, IntPredicate pred) {
		return !pred.test(i - 1) && pred.test(i);
	}

}
