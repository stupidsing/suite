package suite.math.stat;

import suite.primitive.Int_Dbl;

public class Quant {

	public static Int_Dbl filterRange(int start, Int_Dbl fun) {
		return index -> start <= index ? fun.apply(index) : 0d;
	}

	public static float hold(float hold, double ind, double th0, double th1) {
		if (ind <= th0)
			return 1f;
		else if (ind < th1)
			return hold;
		else
			return -1f;
	}

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
