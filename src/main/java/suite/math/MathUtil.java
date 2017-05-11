package suite.math;

import suite.util.To;

public class MathUtil {

	public static float epsilon = .00001f;

	public static void verifyEquals(float f0, float f1) {
		verifyEquals(f0, f1, epsilon);
	}

	public static void verifyEquals(float f0, float f1, float epsilon) {
		float diff = Math.abs(f0 - f1);
		if (!Float.isFinite(diff) || epsilon < diff)
			throw new RuntimeException("Values differ" //
					+ ": f0 = " + To.string(f0) //
					+ ", f1 = " + To.string(f1) //
					+ ", diff = " + diff);
	}

	public static boolean isPositive(double d) {
		return Double.isFinite(d) && 0d <= d;
	}

	public static int steinGcd(int n0, int n1) {
		int shift = 0;

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
				int diff = n1 - n0;
				n1 = n0;
				n0 = diff;
			}
		}

		return n1 << shift;
	}

	public static int steinGcd_(int n0, int n1) {
		if (n0 != 0 && n1 != 0) {
			boolean isEven0 = isEven(n0);
			boolean isEven1 = isEven(n1);

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

	private static boolean isEven(int n) {
		return n % 2 == 0;
	}

}
