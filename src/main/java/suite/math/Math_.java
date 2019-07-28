package suite.math;

import static java.lang.Math.abs;
import static suite.util.Fail.fail;

import suite.util.To;

public class Math_ {

	public static float epsilon = .00001f;

	public static boolean isPositive(double d) {
		return isPositive_(d);
	}

	public static String posNeg(double d) {
		return isPositive_(d) ? "+" : "-";
	}

	public static int steinGcd(int n0, int n1) {
		var shift = 0;

		while (isEven(n0) && isEven(n1)) {
			n0 /= 2;
			n1 /= 2;
			shift++;
		}

		while (isEven(n0))
			n0 /= 2;

		// n0 is odd here
		while (0 < n0) {
			while (isEven(n1))
				n1 /= 2;

			if (n1 < n0)
				n0 -= n1;
			else {
				var diff = n1 - n0;
				n1 = n0;
				n0 = diff;
			}
		}

		return n1 << shift;
	}

	public static int steinGcd_(int n0, int n1) {
		if (n0 != 0 && n1 != 0) {
			var isEven0 = isEven(n0);
			var isEven1 = isEven(n1);

			if (isEven0 && isEven1)
				return 2 * steinGcd_(n0 / 2, n1 / 2);
			else if (isEven0)
				return steinGcd_(n0 / 2, n1);
			else if (isEven1)
				return steinGcd_(n0, n1 / 2);
			else if (n0 < n1)
				return steinGcd_(n0, (n1 - n0) / 2);
			else
				return steinGcd_(n1, (n0 - n1) / 2);
		} else
			return n0 + n1;
	}

	public static void verifyEquals(float f0, float f1) {
		verifyEquals((double) f0, (double) f1, epsilon);
	}

	public static void verifyEquals(float f0, float f1, float epsilon) {
		verifyEquals((double) f0, (double) f1, (double) epsilon);
	}

	public static void verifyEquals(double f0, double f1) {
		verifyEquals(f0, f1, epsilon);
	}

	public static void verifyEquals(double f0, double f1, double epsilon) {
		var diff = abs(f0 - f1);
		if (!Double.isFinite(diff) || epsilon < diff)
			fail("values differ" //
					+ ": f0 = " + To.string(f0) //
					+ ", f1 = " + To.string(f1) //
					+ ", diff = " + diff);
	}

	private static boolean isEven(int n) {
		return n % 2 == 0;
	}

	private static boolean isPositive_(double d) {
		return Double.isFinite(d) && 0d <= d;
	}

}
