package suite.math;

public class MathUtil {

	public static float epsilon = .00001f;

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

	public static int steinGcd0(int n0, int n1) {
		if (n0 != 0 && n1 != 0) {
			boolean isEven0 = isEven(n0);
			boolean isEven1 = isEven(n1);

			if (isEven0 && isEven1)
				return 2 * steinGcd0(n0 / 2, n1 / 2);
			else if (isEven0)
				return steinGcd0(n0 / 2, n1);
			else if (isEven1)
				return steinGcd0(n0, n1 / 2);
			else if (n0 < n1)
				return steinGcd0(n0, (n1 - n0) / 2);
			else
				return steinGcd0(n1, (n0 - n1) / 2);
		} else
			return n0 + n1;
	}

	private static boolean isEven(int n) {
		return n % 2 == 0;
	}

}
