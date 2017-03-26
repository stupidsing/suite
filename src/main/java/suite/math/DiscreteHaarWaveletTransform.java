package suite.math;

// https://en.wikipedia.org/wiki/Discrete_wavelet_transform
public class DiscreteHaarWaveletTransform {

	// assumes input.length >= 2 and input.length = 2^n
	public int[] dhwt(int input[]) {
		int length2 = input.length;
		int output[] = new int[length2];

		while (1 < length2) {
			int length = length2 / 2;
			for (int i = 0; i < length; i++) {
				int i2 = i * 2;
				int a = input[i2];
				int b = input[i2 + 1];
				output[i] = a + b;
				output[i + length] = a - b;
			}

			System.arraycopy(output, 0, input, 0, length2);
			length2 = length;
		}

		return input;
	}

	public int[] idhwt(int input[]) {
		int length = 1, length2;
		int output[] = new int[input.length];

		while ((length2 = length * 2) <= input.length) {
			for (int i = 0; i < length; i++) {
				int sum = input[i];
				int diff = input[i + length];
				int i2 = i * 2;
				output[i2] = (sum + diff) / 2;
				output[i2 + 1] = (sum - diff) / 2;
			}

			System.arraycopy(output, 0, input, 0, length2);
			length = length2;
		}

		return input;
	}

}
