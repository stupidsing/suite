package suite.math.transform;

import suite.util.To;

public class DiscreteCosineTransform {

	private FastFourierTransform fft = new FastFourierTransform();

	// http://dsp.stackexchange.com/questions/2807/fast-cosine-transform-via-fft
	public float[] dct(float[] fs0) {
		var size = fs0.length;
		var size21 = size * 2 - 1;
		var fs1 = new float[size * 8];

		// signal [a, b, c, d] becomes
		// [0, a, 0, b, 0, c, 0, d, 0, d, 0, c, 0, b, 0, a]
		for (var i = 0; i < size; i++) {
			var j = size21 - i;
			var v = fs0[i];
			fs1[i * 4 + 2] = v;
			fs1[j * 4 + 2] = v;
		}

		// take the FFT to get the spectrum
		// [A, B, C, D, 0, -D, -C, -B, -A, -B, -C, -D, 0, D, C, B]
		var fs2 = fft.fft(fs1);

		// throw away everything but the first [A, B, C, D]
		var fs3 = To.vector(size, i -> fs2[i * 2]);

		// and you are done
		return fs3;
	}

	public float[] idct(float[] fs3) {
		var size = fs3.length;
		var size0 = 0;
		var size4 = size * 4;
		var fs2 = new float[size * 8];

		for (var i = 0; i < size; i++) {
			var i2 = i * 2;
			var v = fs3[i];
			fs2[size0 + i2] = v;
			fs2[size4 + i2] = -v;
			if (0 < i) {
				var j2 = size4 - i2;
				fs2[size0 + j2] = -v;
				fs2[size4 + j2] = v;
			}
		}

		var fs1 = fft.ifft(fs2);
		var fs0 = To.vector(size, i -> fs1[i * 4 + 2]);
		return fs0;
	}

}
