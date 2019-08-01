package suite.ts;

import static java.lang.Math.log1p;
import static java.lang.Math.max;
import static java.lang.Math.min;

import java.util.function.IntPredicate;

import primal.primitive.IntFltPredicate;
import primal.primitive.IntFlt_Flt;
import primal.primitive.Int_Dbl;
import suite.trade.Trade_;

public class Quant {

	public static double div(double n, double d0) {
		var d1 = d0 != 0d ? d0 : Trade_.negligible;
		return n / d1;
	}

	public static float div(float n, float d0) {
		var d1 = d0 != 0d ? d0 : Trade_.negligible;
		return n / d1;
	}

	public static Int_Dbl enterExit( //
			int start, int end, //
			int timedExit, //
			IntPredicate isEnterShort, IntPredicate isEnterLong, //
			IntPredicate isExitShort, IntPredicate isExitLong) {
		var isKeepShort = isExitShort.negate();
		var isKeepLong = isExitLong.negate();
		return enterKeep(start, end, timedExit, isEnterShort, isEnterLong, isKeepShort, isKeepLong);
	}

	public static Int_Dbl enterKeep( //
			int start, int end, //
			IntPredicate isEnterShort, IntPredicate isEnterLong, //
			IntPredicate isKeepShort, IntPredicate isKeepLong) {
		return enterKeep(start, end, Integer.MAX_VALUE, isEnterShort, isEnterLong, isKeepShort, isKeepLong);
	}

	private static Int_Dbl enterKeep( //
			int start, int end, //
			int timedExit, //
			IntPredicate isEnterShort, IntPredicate isEnterLong, //
			IntPredicate isKeepShort, IntPredicate isKeepLong) {
		return fold(start, end, timedExit, (i, hold) -> {
			if (hold == 0f)
				return isEnterShort.test(i) ? -1f : isEnterLong.test(i) ? 1f : hold;
			else
				return (hold < 0f ? isKeepShort : isKeepLong).test(i) ? hold : 0f;
		});
	}

	// manual enter, auto exit when draw-down exceeded threshold
	public static Int_Dbl enterUntilDrawDown( //
			float[] prices, //
			double exitThreshold, //
			IntFltPredicate isEnterShort, //
			IntFltPredicate isEnterLong) {
		var length = prices.length;
		var holds = new float[length];
		var hold = 0f;
		var min = Float.MAX_VALUE;
		var max = Float.MIN_VALUE;

		for (var i = 0; i < length; i++) {
			var price = prices[i];
			min = min(min, price);
			max = max(max, price);
			if (hold < 0f) // exit short
				hold = Quant.return_(min, price) < exitThreshold ? hold : 0f;
			else if (0f < hold) // exit long
				hold = Quant.return_(price, max) < exitThreshold ? hold : 0f;
			else if (isEnterShort.test(i, price)) {
				hold = -1f;
				min = price;
			} else if (isEnterLong.test(i, price)) {
				hold = 1f;
				max = price;
			}
			holds[i] = hold;
		}

		return index -> holds[index - 1];
	}

	public static Int_Dbl filterRange(int start, Int_Dbl fun) {
		return index -> start <= index ? fun.apply(index) : 0d;
	}

	public static Int_Dbl fold(int start, int end, IntFlt_Flt fun) {
		return fold(start, end, Integer.MAX_VALUE, fun);
	}

	// hold with fixed day exit
	public static Int_Dbl fold(int start, int end, int nDaysExit, IntFlt_Flt fun) {
		var holds = new float[end];
		var hold = 0f;
		var nDays = 0;
		for (var i = start; i < end; i++) {
			var hold1 = nDays < nDaysExit ? fun.apply(i, hold) : 0f;
			nDays = hold1 != 0f && hold == hold1 ? nDays + 1 : 0;
			holds[i] = hold = hold1;
		}
		return filterRange(1, index -> (double) holds[index - 1]);
	}

	public static float hold(float hold, double ind, double th0, double th1) {
		if (ind <= th0)
			return 1f;
		else if (ind < th1)
			return hold;
		else
			return -1f;
	}

	// enter long when below low-threshold; exit when reached middle-threshold
	// enter short when above hi-threshold; exit when reached middle-threshold
	public static float hold(float hold, double ind, double th0, double th1, double th2) {
		if (ind <= th0)
			return 1f;
		else if (ind < th1)
			return max(0f, hold);
		else if (ind < th2)
			return min(0f, hold);
		else
			return -1f;
	}

	public static int log2trunc(int length0) {
		var size = 1;
		int size1;
		while ((size1 = size << 1) <= length0)
			size = size1;
		return size;
	}

	public static double logReturn(double price0, double price1) {
		return log1p(return_(price0, price1));
	}

	public static double return_(double price0, double price1) {
		return (price1 - price0) / price0;
	}

	public static int sign(double d) {
		return sign(0d, d);
	}

	public static int sign(double d0, double d1) {
		if (d0 < d1)
			return 1;
		else if (d1 < d0)
			return -1;
		else
			return 0;
	}

	public static int sign(int i) {
		return sign(0, i);
	}

	public static int sign(int i0, int i1) {
		if (i0 < i1)
			return 1;
		else if (i1 < i0)
			return -1;
		else
			return 0;
	}

}
