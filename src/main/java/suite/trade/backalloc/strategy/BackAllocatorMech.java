package suite.trade.backalloc.strategy;

import java.util.function.IntPredicate;

import primal.primitive.IntInt_Int;
import suite.streamlet.Read;
import suite.streamlet.Streamlet2;
import suite.trade.analysis.MovingAverage;
import suite.trade.analysis.Oscillator;
import suite.trade.backalloc.BackAllocator;
import suite.ts.BollingerBands;
import suite.ts.Quant;

/**
 * "Mechanical Trading Systems."
 *
 * @author ywsing
 */
public class BackAllocatorMech {

	public static BackAllocatorMech me = new BackAllocatorMech();

	public final Streamlet2<String, BackAllocator> baByName = Read //
			.<String, BackAllocator> empty2() //
			.cons("bb", bollingerBands(20)) //
			.cons("chanbrk", channelBreakout(20)) //
			.cons("dmi", dmi(10)) //
			.cons("dmiadx", dmiAdx(10, 9)) //
			.cons("ma2", ma2(9, 26)) //
			.cons("ma2i", ma2Ichimoku(9, 26)) //
			.cons("ma3", ma3(9, 26, 52)) //
			.cons("ma3i", ma3ichimoku(9, 26, 52)) //
			.cons("macd", macd(9, 12, 26)) //
			.cons("bbadx", mrBbAdx(20, 9)) //
			.cons("bbma200", mrBbMa200(20)) //
			.cons("rsima200", mrRsiMa200(9, 14)) //
			.cons("ssecci", mrSseCci()) //
			.cons("sseccitx", mrSseCciTimedExit()) //
			.cons("p7rev", period7reversal()) //
			.cons("rsix", rsiCrossover(14));

	private BollingerBands bb = new BollingerBands();
	private MovingAverage ma = new MovingAverage();
	private Oscillator osc = new Oscillator();

	private BackAllocatorMech() {
	}

	private BackAllocator bollingerBands(int d20) {
		return BackAllocator_.byPrices(prices -> {
			var sds = bb.bb(prices, 20, 0, 2f).sds;
			return Quant.fold(0, sds.length, (i, hold) -> -Quant.hold(hold, sds[i], -.5d, 0d, .5d));
		});
	}

	private BackAllocator channelBreakout(int d20) {
		return BackAllocator_.byPrices(prices -> {
			var movingRanges = ma.movingRange(prices, d20);

			return Quant.fold(0, prices.length, (i, hold) -> {
				var movingRange = movingRanges[i];
				return -Quant.hold(hold, prices[i], movingRange.min, movingRange.max);
			});
		});
	}

	private BackAllocator dmi(int d10) {
		return BackAllocator_.byDataSource(ds -> {
			var dmis = osc.dmi(ds, 10).dmi;
			return Quant.fold(0, dmis.length, (i, hold) -> -Quant.hold(hold, dmis[i], -.2d, 0d, .2d));
		});
	}

	private BackAllocator dmiAdx(int d10, int d9) {
		return BackAllocator_.byDataSource(ds -> {
			var dmi = osc.dmi(ds, d10);
			var dmis = dmi.dmi;
			var adxs = dmi.adx(d9);
			return Quant.fold(0, dmis.length, (i, hold) -> .2d <= adxs[i] ? -Quant.hold(hold, dmis[i], -.2d, 0d, .2d) : 0f);
		});
	}

	private BackAllocator macd(int d9, int d12, int d26) {
		return BackAllocator_.byPrices(prices -> {
			var macd = ma.macd(prices, d26, d12, d9);
			return index -> {
				var last = index - 1;
				var macd_ = macd.macds[last];
				var movingAvgMacd = macd.movingAvgMacds[last];
				var sign0 = Quant.sign(movingAvgMacd, macd_);
				return sign0 == Quant.sign(0d, movingAvgMacd) ? (double) sign0 : 0d;
			};
		});
	}

	// two moving average cross-over
	private BackAllocator ma2(int d9, int d26) {
		return BackAllocator_.byPrices(prices -> {
			var movingAvgs0 = ma.movingAvg(prices, d26);
			var movingAvgs1 = ma.movingAvg(prices, d9);
			return Quant.filterRange(1, index -> {
				var last = index - 1;
				return (double) Quant.sign(movingAvgs0[last], movingAvgs1[last]);
			});
		});
	}

	// Ichimoku two moving average cross-over
	private BackAllocator ma2Ichimoku(int d9, int d26) {
		return BackAllocator_.byPrices(prices -> {
			var movingAvgs0 = ma.movingAvg(prices, d26);
			var movingAvgs1 = ma.movingAvg(prices, d9);
			return Quant.filterRange(2, index -> {
				var last = index - 1;
				var movingAvg0 = movingAvgs0[last];
				var movingAvg1 = movingAvgs1[last];
				var movingAvg0ytd = movingAvgs0[last - 1];
				var sign = Quant.sign(movingAvg0, movingAvg1);
				return sign == Quant.sign(movingAvg0ytd, movingAvg0) ? (double) sign : 0d;
			});
		});
	}

	// three moving average cross-over
	private BackAllocator ma3(int d9, int d26, int d52) {
		return BackAllocator_.byPrices(prices -> {
			var movingAvgs0 = ma.movingAvg(prices, d52);
			var movingAvgs1 = ma.movingAvg(prices, d26);
			var movingAvgs2 = ma.movingAvg(prices, d9);
			return Quant.filterRange(1, index -> {
				var last = index - 1;
				var movingAvg0 = movingAvgs0[last];
				var movingAvg1 = movingAvgs1[last];
				var movingAvg2 = movingAvgs2[last];
				var sign0 = Quant.sign(movingAvg0, movingAvg1);
				var sign1 = Quant.sign(movingAvg1, movingAvg2);
				return sign0 == sign1 ? (double) -sign0 : 0d;
			});
		});
	}

	// Ichimoku three moving average cross-over
	private BackAllocator ma3ichimoku(int d9, int d26, int d52) {
		return BackAllocator_.byPrices(prices -> {
			var length = prices.length;
			var movingAvgs0 = ma.movingAvg(prices, d52);
			var movingAvgs1 = ma.movingAvg(prices, d26);
			var movingAvgs2 = ma.movingAvg(prices, d9);

			return Quant.fold(1, length, (i, hold) -> {
				var im1 = i - 1;
				var movingAvg0 = movingAvgs0[i];
				var movingAvg1 = movingAvgs1[i];
				var movingAvg2 = movingAvgs2[i];
				var movingAvg1ytd = movingAvgs1[im1];
				var movingAvg2ytd = movingAvgs2[im1];
				var sign0 = Quant.sign(movingAvg0, movingAvg1);
				var sign1 = Quant.sign(movingAvg1, movingAvg2);
				var sign2 = Quant.sign(movingAvg1, movingAvg1ytd);
				var sign3 = Quant.sign(movingAvg2, movingAvg2ytd);
				var b1 = sign0 == sign1 && sign1 == sign2 && (hold != 0f || sign2 == sign3);
				return b1 ? (float) -sign0 : 0f;
			});
		});
	}

	private BackAllocator mrBbAdx(int d20, int d9) {
		return BackAllocator_ //
				.byDataSource(ds -> {
					var prices = ds.prices;
					var bb_ = bb.bb(prices, d20, 0, 2f);
					var adxs = osc.dmi(ds).adx(d9);

					return Quant.enterKeep(1, prices.length, //
							i -> adxs[i] < .2f && cross(i, bb_.uppers, prices), //
							i -> adxs[i] < .2f && cross(i, prices, bb_.lowers), //
							i -> true, //
							i -> true);
				}) //
				.stop(.9875d, .9875d);
	}

	private BackAllocator mrBbMa200(int d20) {
		return BackAllocator_ //
				.byPrices(prices -> {
					var movingAvgs = ma.movingAvg(prices, 200);
					var bb_ = bb.bb(prices, d20, 0, 2f);
					var lowers = bb_.lowers;
					var uppers = bb_.uppers;
					var sds = bb_.sds;

					return Quant.enterKeep(1, prices.length, //
							i -> cross(i, uppers, prices) && prices[i] < movingAvgs[i], //
							i -> cross(i, prices, lowers) && movingAvgs[i] < prices[i], //
							i -> 0f <= sds[i], //
							i -> sds[i] <= 0f);
				}) //
				.stopLoss(.975d);
	}

	private BackAllocator mrRsiMa200(int d9, int d14) {
		return BackAllocator_ //
				.byPrices(prices -> {
					var movingAvgs = ma.movingAvg(prices, 200);
					var rsi0 = osc.rsi(prices, d14);
					var rsi1 = osc.rsi(prices, d9);

					return Quant.enterExit(1, prices.length, //
							Integer.MAX_VALUE, //
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
						var n = 0;
						for (var i = s; i < e; i++)
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

	private BackAllocator rsiCrossover(int d14) {
		return BackAllocator_ //
				.byPrices(prices -> {
					var rsi = osc.rsi(prices, d14);

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
					var prices = ds.prices;
					var stos = osc.stochastic(ds, 14);
					var stoSlows = ma.movingAvg(stos, 3);
					var ccis = osc.cci(ds, 10);

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
