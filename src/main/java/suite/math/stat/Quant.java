package suite.math.stat;

import java.util.function.IntPredicate;

import suite.primitive.IntFlt_Flt;
import suite.primitive.Int_Dbl;
import suite.trade.Trade_;

public class Quant {

	public static double div(double n, double d0) {
		double d1 = d0 != 0d ? d0 : Trade_.negligible;
		return n / d1;
	}

	public static float div(float n, float d0) {
		float d1 = d0 != 0d ? d0 : Trade_.negligible;
		return n / d1;
	}

	public static Int_Dbl filterRange(int start, Int_Dbl fun) {
		return index -> start <= index ? fun.apply(index) : 0d;
	}

	public static Int_Dbl enterExit( //
			int start, int end, //
			int timedExit, //
			IntPredicate isEnterShort, IntPredicate isEnterLong, //
			IntPredicate isExitShort, IntPredicate isExitLong) {
		IntPredicate isKeepShort = isExitShort.negate();
		IntPredicate isKeepLong = isExitLong.negate();
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

	public static Int_Dbl fold(int start, int end, IntFlt_Flt fun) {
		return fold(start, end, Integer.MAX_VALUE, fun);
	}

	// hold with fixed day exit
	public static Int_Dbl fold(int start, int end, int nDaysExit, IntFlt_Flt fun) {
		int nDays = 0;
		float[] holds = new float[end];
		float hold = 0f;
		for (int i = start; i < end; i++) {
			float hold1 = fun.apply(i, hold);
			nDays += hold != hold1 ? 1 : 0;
			if (nDaysExit < nDays) {
				hold1 = 0f;
				nDays = 0;
			}
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
			return Math.max(0f, hold);
		else if (ind < th2)
			return Math.min(0f, hold);
		else
			return -1f;
	}

	public static double logReturn(double price0, double price1) {
		return Math.log1p(return_(price0, price1));
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
