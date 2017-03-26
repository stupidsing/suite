package suite.math;

// https://en.wikipedia.org/wiki/Discrete_wavelet_transform
public class DiscreteHaarWaveletTransform {

	// assumes input.length >= 2 and input.length = 2^n
	public static int[] dhwt(int input[]) {
		for (int length = input.length >> 1; length > 1; length >>= 1) {
			int output[] = new int[input.length];

			for (int i = 0; i < length; ++i) {
				int i2 = i * 2;
				int a = input[i2];
				int b = input[i2 + 1];
				output[i] = a + b;
				output[length + i] = a - b;
			}

			input = output;
		}

		return input;
	}

}
