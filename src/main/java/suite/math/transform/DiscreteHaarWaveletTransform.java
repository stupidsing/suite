package suite.math.transform;

import suite.primitive.Ints_;

// https://en.wikipedia.org/wiki/Discrete_wavelet_transform
public class DiscreteHaarWaveletTransform {

	// assumes input.length >= 2 and input.length = 2^n
	public int[] dhwt(int[] input) {
		var length2 = input.length;
		var output = new int[length2];

		while (1 < length2) {
			var length = length2 / 2;
			for (var i = 0; i < length; i++) {
				var i2 = i * 2;
				var a = input[i2 + 0];
				var b = input[i2 + 1];
				output[i + 0] = a + b;
				output[i + length] = a - b;
			}

			Ints_.copy(output, 0, input, 0, length2);
			length2 = length;
		}

		return input;
	}

	public int[] idhwt(int[] input) {
		int length = 1, length2;
		var output = new int[input.length];

		while ((length2 = length * 2) <= input.length) {
			for (var i = 0; i < length; i++) {
				var sum_ = input[i + 0];
				var diff = input[i + length];
				var i2 = i * 2;
				output[i2 + 0] = (sum_ + diff) / 2;
				output[i2 + 1] = (sum_ - diff) / 2;
			}

			Ints_.copy(output, 0, input, 0, length2);
			length = length2;
		}

		return input;
	}

}
