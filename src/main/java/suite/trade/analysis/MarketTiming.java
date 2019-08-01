package suite.trade.analysis;

import static java.lang.Math.abs;
import static java.lang.Math.max;
import static suite.util.Streamlet_.forInt;

import suite.math.numeric.Statistic;
import suite.streamlet.As;

public class MarketTiming {

	public final int strgBear = 1 << 4;
	public final int weakBear = 1 << 3;
	public final int rngBound = 1 << 2;
	public final int weakBull = 1 << 1;
	public final int strgBull = 1 << 0;

	private MovingAverage ma = new MovingAverage();
	private Statistic stat = new Statistic();

	public float[] hold(float[] prices, float h0, float h1, float h2) {
		var flags = time(prices);
		var length = flags.length;
		var holds = new float[length];
		var hold = 0f;

		for (var i = 0; i < length; i++) {
			if ((flags[i] & strgBear) != 0)
				hold = -h2;
			else if ((flags[i] & strgBull) != 0)
				hold = +h2;
			else if ((flags[i] & weakBear) != 0)
				hold = -h1;
			else if ((flags[i] & weakBull) != 0)
				hold = +h1;
			else
				hold = h0;
			holds[i] = hold;
		}

		return holds;
	}

	public int[] time(float[] prices) {
		var length = prices.length;
		var lookback = 40;

		var ma20 = ma.movingAvg(prices, 20);
		var ma50 = ma.movingAvg(prices, 50);
		var lookback80 = lookback * .8d;
		var flags = new int[length];

		for (var i = 0; i < length; i++) {
			var past = max(0, i - lookback);
			var past_i = forInt(past, i);
			var past1_i = past_i.drop(1);

			var ma20abovema50 = past_i.filter(j -> ma50[j] < ma20[j]).size();
			var ma50abovema20 = past_i.filter(j -> ma20[j] < ma50[j]).size();
			var r = ma50abovema20 / (double) ma20abovema50;

			var isStrglyBullish = true //
					&& lookback <= ma20abovema50 //
					&& past1_i.isAll(j -> ma20[j - 1] <= ma20[j]) //
					&& past1_i.isAll(j -> ma50[j - 1] <= ma50[j]) //
					&& (1.02d * ma50[i] <= ma20[i] || ma20[past] - ma50[past] < ma20[i] - ma50[i]) //
					&& past_i.isAll(j -> ma20[j] <= prices[j]);

			var isWeaklyBullish = true //
					&& lookback80 <= ma20abovema50 //
					&& past1_i.isAll(j -> ma50[j - 1] <= ma50[j]) //
					&& past_i.isAll(j -> ma50[j] <= prices[j]);

			var isStrglyBearish = true //
					&& lookback <= ma50abovema20 //
					&& past1_i.isAll(j -> ma20[j] <= ma20[j - 1]) //
					&& past1_i.isAll(j -> ma50[j] <= ma50[j - 1]) //
					&& (1.02d * ma20[i] <= ma50[i] || ma50[past] - ma20[past] < ma50[i] - ma20[i]) //
					&& past_i.isAll(j -> prices[j] <= ma20[j]);

			var isWeaklyBearish = true //
					&& lookback80 <= ma50abovema20 //
					&& past1_i.isAll(j -> ma50[j] <= ma50[j - 1]) //
					&& past_i.isAll(j -> prices[j] <= ma50[j]);

			var isRangeBound___ = true // non-trending
					&& 2d / 3d <= r && r <= 3d / 2d //
					&& stat.meanVariance(past_i.collect(As.floats(j -> ma50[j])).toArray()).volatility() < .02d //
					&& .02d < stat.meanVariance(past_i.collect(As.floats(j -> ma20[j])).toArray()).volatility() //
					&& (ma20[i] + ma50[i]) * .02d <= abs(ma20[i] - ma50[i]);

			var flag = 0 //
					+ (isStrglyBearish ? strgBear : 0) //
					+ (isWeaklyBearish ? weakBear : 0) //
					+ (isRangeBound___ ? rngBound : 0) //
					+ (isWeaklyBullish ? weakBull : 0) //
					+ (isStrglyBullish ? strgBull : 0);

			flags[i] = flag;
		}

		return flags;
	}

}
